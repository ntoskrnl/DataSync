package com.cardiomood.android.sync.ormlite;

import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

public class SyncDAO<T extends SyncEntity, ID> extends BaseDaoImpl<T, ID> {

    protected SyncDAO(ConnectionSource connectionSource, Class<T> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

    public T findBySyncId(String syncId) throws SQLException {
        return SyncHelper.findBySyncId(this, syncId);
    }

}
