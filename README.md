[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.cardiomood.android/android-data-sync/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/com.cardiomood.android/android-data-sync) DataSync
========

Synchronization between Parse.com and local Android database made easy. 

The library provides annotations and utility classes to help you convert your Parse objects to OrmLite entities.

## Add to your project
If you are using gradle, add the following line to the <code>dependecies</code> section of your **build.gradle**:
```gradle
compile 'com.cardiomood.android:android-data-sync:0.2'
```

In maven add the following dependecy to **pom.xml**:
```xml
<dependency>
  <groupId>com.cardiomood.android</groupId>
  <artifactId>android-data-sync</artifactId>
  <version>0.2</version>
  <type>aar</type>
</dependency>
```

This library also requires Parse Android SDK.

## Configuration

Assuming your app is already configured to work with Android Parse.

All you need to do is to extend <code>com.cardiomood.android.sync.ormlite.SyncEntity</code> for each
entity class you would like to enable synchronization. Here is an example of an entity class:

```java
import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.ormlite.SyncEntity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

// This class will be mapped to a Parse class named "Example"
@ParseClass(name = "Example")
@DatabaseTable(tableName = "examples", daoClass = ExampleDAO.class)
public class ExampleEntity extends SyncEntity implements Serializable {

    /** Local ID field of this entity */
    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    // mapped to the "name" field of ParseObject
    @ParseField(name = "name")
    @DatabaseField(columnName = "name")
    private String exampleName;

    // mapped to the "typeId" field of ParseObject
    @DatabaseField(columnName = "type_id")
    @ParseField
    private Long typeId;

    // mapped to the "lastViewDate" field of ParseObject
    @DatabaseField(columnName = "last_view_date", dataType = DataType.DATE_LONG)
    @ParseField(name = "lastViewDate")
    private Date lastViewed;
    
    public ExampleEntity() {
      // public constructor with no arguments required
    }
    
    // getters and setters here
    
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

Synchronization is performed only between objects that have been modified since the latest successful
synchronization. At the moment, it's completely up to you to keep track of synchronization date.
If you don't specify <code>lastSyncDate</code>, the library will attempt to synchronize all objects.

You must also update <code>syncDate</code> of your local objects, as this field represents last modification date.

In your background code:
```java
// save date point
Date syncDate = new Date();

// synchronized
syncHelper.synObjects(ExampleEntity.class, false, new SyncHelper.SyncCallback<ExampleEntity>() {
  
  @Override
  public void onSaveLocally(ExampleEntity localObject, ParseObject remoteObject) {
    // invoked before localObject is persisted locally
  }
  
  @Override
  public void onSaveRemotely(ExampleEntity localObject, ParseObject remoteObject) {
    // invoked before remoteObject is saved remotely
  }

});

// if there were no exceptions, persist syncDate to Android preferences (or to local DB)
...
```
In the example above, class <code>Example</code> is a custom subclass of <code>ParseObject</code>.
