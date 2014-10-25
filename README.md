[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.cardiomood.android/android-data-sync/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.cardiomood.android/android-data-sync) DataSync
========

Synchronization between Parse.com and local Android database made easy. 

The library provides annotations and utility classes to help you convert your Parse objects to OrmLite entities.

## Add to your project
If you are using gradle, add the following line to the <code>dependecies</code> section of your **build.gradle**:
```gradle
compile 'com.cardiomood.android:android-data-sync:0.1'
```

In maven add the following dependecy to **pom.xml**:
```xml
<dependency>
  <groupId>com.cardiomood.android</groupId>
  <artifactId>android-data-sync</artifactId>
  <version>0.1</version>
  <type>aar</type>
</dependency>
```

## Configuration

Assuming your app is already configured to work with Android Parse. To configure DataSync library you need to do the following:

1. Your database helper class should extend <code>com.cardiomood.android.sync.ormlite.SyncDatabaseHelper</code>.
2. All your entity classes must extends <code>com.cardiomood.android.sync.ormlite.SyncEntity</code>.
3. For each entity-class implement a custom DAO-class that extend <code>SyncDAO<SyncEntity, Long></code>.

### Step 1. Implementing DatabaseHelper

```java
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.cardiomood.android.sync.ormlite.SyncDatabaseHelper;
import com.cardiomood.android.sync.ormlite.SyncEntity;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class DatabaseHelper extends SyncDatabaseHelper {

    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String DATABASE_NAME   = "example.db";
    private static final int DATABASE_VERSION   = 1;

    private Context mContext;

    private ExampleDAO exampleDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }


    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        Log.d(TAG, "onCreate()");
        try {
            TableUtils.createTable(connectionSource, ExampleEntity.class);
        } catch (SQLException ex) {
            Log.e(TAG, "onCreate(): failed to create tables", ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        // Upgrade or downgrade the database schema
    }

    public Context getContext() {
        return mContext;
    }

    public ExampleDAO getExampleDao() throws SQLException {
        if (exampleDao == null) {
            exampleDao = new ExampleDAO(getConnectionSource(), ExampleEntity.class);
        }
        return exampleDao;
    }

    @Override
    public <T extends SyncEntity> SyncDAO<T, Long> getSyncDao(Class<T> clazz) throws SQLException {
        if (ExampleEntity.class.equals(clazz))
            return (SyncDAO<T, Long>) getExampleDao();

        // not supported class!!!
        throw new IllegalArgumentException("Class " + clazz + " is not supported!");
    }

}
```

### Step 2. Implementing SyncEntity

```java
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@ParseClass(name = "Example") // Mapped to Parse class named "Example"
@DatabaseTable(tableName = "examples", daoClass = ExampleDAO.class)
public class ExampleEntity extends SyncEntity implements Serializable {

    @ParseField(name = "name") // mapped to the "name" field of ParseObject
    @DatabaseField(columnName = "name")
    private String exampleName;

    @DatabaseField(columnName = "type_id")
    @ParseField // mapped to the "typeId" field of ParseObject
    private Long typeId;

    @DatabaseField(columnName = "last_view_date", dataType = DataType.DATE_LONG)
    @ParseField(name = "lastViewDate")
    private Date lastViewed;
    
    public ExampleEntity() {
      // public constructor with no arguments required
    }
    
    // getters and setters here
    
}
```

### Step 3. Extend SyncDao

```java
import com.cardiomood.android.sync.ormlite.SyncDAO;
import com.j256.ormlite.support.ConnectionSource;

public class ExampleDAO extends SyncDAO<ExampleEntity, Long> {

    public ExampleDAO(ConnectionSource connectionSource, Class<ExampleEntity> dataClass) throws SQLException {
        super(connectionSource, dataClass);
    }

}
```

## How to use

Initialize SyncHelper object somewhere in your <code>onCreate()</code> method:
```java

// obtain our DatabaseHelper object
SyncDatabaseHelper dbHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);

// create and initialize SyncHelper object
SyncHelper syncHelper = new SyncHelper(dbHelper); 
syncHelper.setUserId(ParseUser.getCurrentUser().getObjectId());
syncHelper.setLastSyncDate(new Date(lastSyncDate));
```

Synchronization is performed only between objects that were updated since the latest successful synchronization. At the moment, it's completely up to you to keep track of synchronization date. If you don't specify <code>lastSyncDate</code>, the library will attempt to synchronize all objects.

In your background code:
```java
// save date point
Date syncDate = new Date();

// synchronized
syncHelper.synObjects(Example.class, ExampleEntity.class, false, new SyncHelper.SyncCallback() {
  
  @Override
  public void onSaveLocally(ExampleEntity localObject, Example remoteObject) {
    // invoked befor localObject is persisted
  }
  
  @Override
  public void onSaveRemotely(ExampleEntity localObject, Example remoteObject) {
    // invoked befor remoteObject is saved
  }

});

// if there were no exceptions, persist syncDate to Preferences
...
```
In the example above, <code>Example</code> class is a custom subclass of <code>ParseObject</code>. Version 0.1 of SyncData requires you to extend <code>ParseObject</code> class.
