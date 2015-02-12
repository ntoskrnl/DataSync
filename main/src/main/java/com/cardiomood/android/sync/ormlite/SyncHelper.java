package com.cardiomood.android.sync.ormlite;

import android.util.Pair;

import com.cardiomood.android.sync.SyncException;
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.parse.ParseTools;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by antondanhsin on 24/10/14.
 */
public class SyncHelper {

    private static final String PARSE_USER_ID_FIELD = "userId";
    private static final String PARSE_UPDATED_AT_FIELD = "updatedAt";
    private static final String PARSE_OBJECT_ID_FIELD = "objectId";
    private static final String PARSE_DELETED_FIELD = "deleted";

    private static final String DB_USER_ID_FIELD = "sync_user_id";
    private static final String DB_UPDATED_AT_FIELD = "sync_timestamp";
    private static final String DB_OBJECT_ID_FIELD = "sync_id";
    private static final String DB_DELETED_FIELD = "is_deleted";

    private Date lastSyncDate = new Date(0);
    private String userId = null;
    private OrmLiteSqliteOpenHelper dbHelper = null;
    private String parseUserIdField = PARSE_USER_ID_FIELD;
    private String localUserIdField = DB_USER_ID_FIELD;


    public SyncHelper(OrmLiteSqliteOpenHelper syncDatabaseHelper) {
        this.dbHelper = syncDatabaseHelper;
    }

    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Date lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getParseUserIdField() {
        return parseUserIdField;
    }

    public void setParseUserIdField(String parseUserIdField) {
        this.parseUserIdField = parseUserIdField;
    }

    public String getLocalUserIdField() {
        return localUserIdField;
    }

    public void setLocalUserIdField(String localUserIdField) {
        this.localUserIdField = localUserIdField;
    }

    public <E extends SyncEntity> void synObjects(Class<E> entityClass) throws SyncException {
        synObjects(entityClass, false, null);
    }

    public <E extends SyncEntity> void synObjects(Class<E> entityClass, boolean userAware) throws SyncException {
        synObjects(entityClass, userAware, null);
    }

    public <E extends SyncEntity> void synObjects(final Class<E> entityClass, boolean userAware,
                                                  final SyncCallback<E> callback) throws SyncException {
        try {
            final String parseClass = extractParseClass(entityClass);

            // get updated remote objects
            ParseQuery query = ParseQuery.getQuery(parseClass);
            query.whereGreaterThan(PARSE_UPDATED_AT_FIELD, lastSyncDate);
            if (userAware && userId != null)
                query.whereEqualTo(PARSE_USER_ID_FIELD, userId);
            //query.orderByAscending(PARSE_OBJECT_ID_FIELD);
            final List<ParseObject> remoteObjects = ParseTools.findAllParseObjects(query);

            // get updated local objects
            final SyncDAO<E, ?> syncDao = dbHelper.getDao(entityClass);
            QueryBuilder<E, ?> dbQuery = syncDao.queryBuilder();
            //dbQuery.orderBy(DB_OBJECT_ID_FIELD, true);
            Where<E, ?> where = dbQuery.where().gt(DB_UPDATED_AT_FIELD, lastSyncDate);
            if (userAware && userId != null)
                where.and().eq(DB_USER_ID_FIELD, userId);
            final List<E> localObjects = where.query();

                    // create local object map
                    Map<String, E> localObjectMap = new HashMap<String, E>(localObjects.size());
                    for (E localObject : localObjects) {
                        localObjectMap.put(localObject.getSyncId(), localObject);
                    }

                    List<Pair<E, ParseObject>> toSaveLocally = new ArrayList<>();
                    List<Pair<E, ParseObject>> toSaveRemotely = new ArrayList<>();

                    for (ParseObject remoteObject : remoteObjects) {
                        String syncId = remoteObject.getObjectId();
                        E localObject = localObjectMap.get(syncId);

                        if (localObject == null) {
                            localObject = findBySyncId(syncDao, syncId);
                            if (localObject == null) {
                                // this object was created on the server but doesn't exist locally
                                localObject = SyncEntity.fromParseObject(remoteObject, entityClass);
                            } else {
                                // the object exists locally but out-of-date
                                SyncEntity.fromParseObject(remoteObject, localObject);
                            }
                            toSaveLocally.add(new Pair<>(localObject, remoteObject));
                            continue;
                        }

                        if (localObject != null) {
                            long localTime = (localObject.getSyncDate() == null)
                                    ? 0L : localObject.getSyncDate().getTime();
                            long remoteTime = (remoteObject.getUpdatedAt() == null)
                                    ? 0L : remoteObject.getUpdatedAt().getTime();

                            if (remoteTime > localTime) {
                                // the remote object is newer
                                SyncEntity.fromParseObject(remoteObject, localObject);
                                toSaveLocally.add(new Pair<>(localObject, remoteObject));
                            } else if (remoteTime < localTime) {
                                // the local objects is newer
                                SyncEntity.toParseObject(localObject, remoteObject);
                                toSaveRemotely.add(new Pair<>(localObject, remoteObject));
                            }
                        }
                    }
                    localObjectMap = null;

                    // create remote object map
                    Map<String, ParseObject> remoteObjectMap = new HashMap<String, ParseObject>(remoteObjects.size());
                    for (ParseObject remoteObject : remoteObjects) {
                        remoteObjectMap.put(remoteObject.getObjectId(), remoteObject);
                    }

                    for (E localObject : localObjects) {
                        String syncId = localObject.getSyncId();
                        if (syncId == null) {
                            // a brand new object!
                            ParseObject remoteObject = SyncEntity.toParseObject(localObject);
                            toSaveRemotely.add(new Pair<>(localObject, remoteObject));
                            continue;
                        }

                        ParseObject remoteObject = remoteObjectMap.get(syncId);
                        if (remoteObject == null && !localObject.isDeleted()) {
                            // object was created locally but doesn't exist or too old on the server
                            // this is weird because syncId is not null

                            // try to get it from server
                            remoteObject = ParseObject.createWithoutData(parseClass, syncId).fetch();
                            toSaveRemotely.add(new Pair<>(localObject, remoteObject));
                            continue;
                        }

                        if (remoteObject != null) {
                            long localTime = (localObject.getSyncDate() == null)
                                    ? 0L : localObject.getSyncDate().getTime();
                            long remoteTime = (remoteObject.getUpdatedAt() == null)
                                    ? 0L : remoteObject.getUpdatedAt().getTime();


                            if (remoteTime > localTime) {
                                // the remote object is newer
                                SyncEntity.fromParseObject(remoteObject, localObject);
                                toSaveLocally.add(new Pair<>(localObject, remoteObject));
                            } else if (remoteTime < localTime) {
                                // the local objects is newer
                                SyncEntity.toParseObject(localObject, remoteObject);
                                toSaveRemotely.add(new Pair<>(localObject, remoteObject));
                            }
                        }
                    }

            if (callback != null) {
                callback.beforeSync(toSaveLocally, toSaveRemotely);
            }

            for (Pair<E, ParseObject> p: toSaveLocally) {
                final E localObject = p.first;
                final ParseObject remoteObject = p.second;
                TransactionManager.callInTransaction(dbHelper.getConnectionSource(), new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        if (callback != null) {
                            callback.onSaveLocally(localObject, remoteObject);
                        }
                        syncDao.createOrUpdate(localObject);
                        return null;
                    }
                });

            }

            for (Pair<E, ParseObject> p: toSaveRemotely) {
                final E localObject = p.first;
                final ParseObject remoteObject = p.second;
                TransactionManager.callInTransaction(dbHelper.getConnectionSource(), new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        if (callback != null) {
                            callback.onSaveRemotely(localObject, remoteObject);
                        }
                        remoteObject.save();
                        if (localObject.getSyncId() == null) {
                            SyncEntity.fromParseObject(remoteObject, localObject);
                            syncDao.createOrUpdate(localObject);
                        }
                        return null;
                    }
                });
            }

            if (callback != null) {
                callback.afterSync();
            }
        } catch (Exception ex) {
            throw new SyncException("Synchronization failed", ex);
        }
    }

    public static <E extends SyncEntity> E findBySyncId(Dao<E, ?> dao, String syncId) throws SQLException {
        return dao.queryForFirst(
                dao.queryBuilder().where().eq(DB_OBJECT_ID_FIELD, syncId).prepare()
        );
    }

    public static String extractParseClass(Class entityClass) {
        ParseClass annotation = (ParseClass) entityClass.getAnnotation(ParseClass.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Class " + entityClass.getName()
                    + " must be annotated with " + ParseClass.class.getName());
        }
        return annotation.name();
    }

    public static interface SyncCallback<E extends SyncEntity> {

        void onSaveLocally(E localObject, ParseObject remoteObject) throws Exception;

        void onSaveRemotely(E localObject, ParseObject remoteObject) throws Exception;

        void beforeSync(List<Pair<E, ParseObject>> toSaveLocally, List<Pair<E, ParseObject>> toSaveRemotely);

        void afterSync();

    }

}
