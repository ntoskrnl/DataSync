package com.cardiomood.android.sync.ormlite;

import com.cardiomood.android.sync.parse.ParseTools;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.sql.SQLException;
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
    private SyncDatabaseHelper syncDatabaseHelper = null;


    public SyncHelper(SyncDatabaseHelper syncDatabaseHelper) {
        this.syncDatabaseHelper = syncDatabaseHelper;
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

    public <P extends ParseObject, E extends SyncEntity> void synObjects(
            final Class<P> parseClass,
            final Class<E> entityClass,
            final boolean userAware,
            final SyncCallback<P, E> callback
    ) throws ParseException, SQLException {

        // get updated remote objects
        ParseQuery<P> query = ParseQuery.getQuery(parseClass);
        query.whereGreaterThan(PARSE_UPDATED_AT_FIELD, lastSyncDate);
        if (userAware && userId != null)
            query.whereEqualTo(PARSE_USER_ID_FIELD, userId);
        //query.orderByAscending(PARSE_OBJECT_ID_FIELD);
        final List<P> remoteObjects = ParseTools.findAllParseObjects(query);

        // get updated local objects
        final SyncDAO<E, Long> syncDao = syncDatabaseHelper.getSyncDao(entityClass);
        QueryBuilder<E, Long> dbQuery = syncDao.queryBuilder();
        //dbQuery.orderBy(DB_OBJECT_ID_FIELD, true);
        Where<E, Long> where = dbQuery.where().gt(DB_UPDATED_AT_FIELD, lastSyncDate);
        if (userAware && userId != null)
            where.and().eq(DB_USER_ID_FIELD, userId);
        final List<E> localObjects = where.query();

        syncDatabaseHelper.callInTransaction(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // create local object map
                Map<String, E> localObjectMap = new HashMap<String, E>(localObjects.size());
                for (E localObject : localObjects) {
                    localObjectMap.put(localObject.getSyncId(), localObject);
                }

                for (P remoteObject : remoteObjects) {
                    String syncId = remoteObject.getObjectId();
                    E localObject = (E) localObjectMap.get(syncId);

                    if (localObject == null && !remoteObject.getBoolean(PARSE_DELETED_FIELD)) {
                        localObject = syncDao.findBySyncId(syncId);
                        if (localObject == null) {
                            // this object was created on the server but doesn't exist locally
                            localObject = SyncEntity.fromParseObject(remoteObject, entityClass);
                        } else {
                            // the object exists locally but out-of-date
                            SyncEntity.fromParseObject(remoteObject, localObject);
                        }
                        if (callback != null) {
                            callback.onSaveLocally(localObject, remoteObject);
                        }
                        syncDao.createOrUpdate(localObject);
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
                            if (callback != null) {
                                callback.onSaveLocally(localObject, remoteObject);
                            }
                            syncDao.update(localObject);
                        } else if (remoteTime < localTime) {
                            // the local objects is newer
                            SyncEntity.toParseObject(localObject, remoteObject);
                            if (callback != null) {
                                callback.onSaveRemotely(localObject, remoteObject);
                            }
                            remoteObject.save();
                        }
                    }
                }
                localObjectMap = null;

                // create remote object map
                Map<String, P> remoteObjectMap = new HashMap<String, P>(remoteObjects.size());
                for (P remoteObject : remoteObjects) {
                    remoteObjectMap.put(remoteObject.getObjectId(), remoteObject);
                }

                for (E localObject : localObjects) {
                    String syncId = localObject.getSyncId();
                    if (syncId == null) {
                        // a brand new object!
                        P remoteObject = (P) SyncEntity.toParseObject(localObject);
                        if (callback != null) {
                            callback.onSaveRemotely(localObject, remoteObject);
                        }
                        remoteObject.save();
                        SyncEntity.fromParseObject(remoteObject, localObject);
                        syncDao.update(localObject);
                        continue;
                    }

                    P remoteObject = remoteObjectMap.get(syncId);
                    if (remoteObject == null && !localObject.isDeleted()) {
                        // object was created locally but doesn't exist or too old on the server
                        // this is weird because syncId is not null

                        // try to get it from server
                        remoteObject = ParseObject.createWithoutData(parseClass, syncId).fetch();
                        SyncEntity.toParseObject(localObject, remoteObject);
                        if (callback != null) {
                            callback.onSaveRemotely(localObject, remoteObject);
                        }
                        remoteObject.save();
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
                            if (callback != null) {
                                callback.onSaveLocally(localObject, remoteObject);
                            }
                            syncDao.update(localObject);
                        } else if (remoteTime < localTime) {
                            // the local objects is newer
                            SyncEntity.toParseObject(localObject, remoteObject);
                            if (callback != null) {
                                callback.onSaveRemotely(localObject, remoteObject);
                            }
                            remoteObject.save();
                        }
                    }
                }

                return null;
            }
        });
    }

    public static interface SyncCallback<P extends ParseObject, E extends SyncEntity> {

        void onSaveLocally(E localObject, P remoteObject);

        void onSaveRemotely(E localObject, P remoteObject);
    }

}
