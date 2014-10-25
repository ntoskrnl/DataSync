package com.cardiomood.android.sync.ormlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.misc.TransactionManager;

import java.sql.SQLException;
import java.util.concurrent.Callable;

/**
 * Created by antondanhsin on 24/10/14.
 */
public abstract class SyncDatabaseHelper extends OrmLiteSqliteOpenHelper {

    public SyncDatabaseHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
    }


    public void callInTransaction(Callable<Void> callable) throws SQLException {
        TransactionManager.callInTransaction(getConnectionSource(), callable);
    }

    public abstract <T extends SyncEntity> SyncDAO<T, Long> getSyncDao(Class<T> clazz) throws SQLException;
}
