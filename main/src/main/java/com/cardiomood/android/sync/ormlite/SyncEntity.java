package com.cardiomood.android.sync.ormlite;

import com.cardiomood.android.sync.annotations.ParseClass;
import com.cardiomood.android.sync.annotations.ParseField;
import com.cardiomood.android.sync.parse.ParseValueConverter;
import com.cardiomood.android.sync.tools.ReflectionUtils;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.parse.ParseObject;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * Created by antondanhsin on 08/10/14.
 */
public abstract class SyncEntity {

    @DatabaseField(columnName = "_id", generatedId = true)
    private Long id;

    @DatabaseField(columnName = "sync_id", unique = true)
    private String syncId;

    @DatabaseField(columnName = "sync_timestamp", dataType = DataType.DATE_LONG)
    private Date syncDate;

    @DatabaseField(columnName = "creation_timestamp", dataType = DataType.DATE_LONG)
    private Date creationDate;

    @DatabaseField(columnName = "deleted", dataType = DataType.BOOLEAN)
    @ParseField(name = "deleted")
    private boolean deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSyncId() {
        return syncId;
    }

    public void setSyncId(String syncId) {
        this.syncId = syncId;
    }

    public Date getSyncDate() {
        return syncDate;
    }

    public void setSyncDate(Date syncDate) {
        this.syncDate = syncDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public static <T extends SyncEntity> void fromParseObject(final ParseObject parseObject, final T entity) {
        try {
            // extract value converter
            Class<? extends SyncEntity> entityClass = entity.getClass();
            ParseClass classAnnotation = entityClass.getAnnotation(ParseClass.class);

            if (classAnnotation == null) {
                throw new IllegalArgumentException("Class " + entityClass.getName() + " must declare annotation " + ParseClass.class.getName());
            }
            // TODO: converters must be cached!
            final ParseValueConverter converter = classAnnotation.valueConverterClass().newInstance();

            ReflectionUtils.doWithFields(
                    entityClass,
                    new ReflectionUtils.FieldCallback() {
                        @Override
                        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                            // extract Parse field name
                            ParseField fieldAnnotation = field.getAnnotation(ParseField.class);
                            if (fieldAnnotation == null)
                                return;
                            String parseFieldName = field.getName();
                            if (fieldAnnotation.name() != null && !fieldAnnotation.name().isEmpty()) {
                                parseFieldName = fieldAnnotation.name();
                            }

                            try {
                                boolean accessible = field.isAccessible();
                                field.setAccessible(true);
                                Object remoteValue = parseObject.get(parseFieldName);
                                Class localValueType = field.getType();

                                field.set(entity, converter.convertValue(remoteValue, localValueType));

                                // restore accessible flag
                                field.setAccessible(accessible);
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to process field "
                                        + field.getName() + " with annotation " + fieldAnnotation);
                            }
                        }
                    }
            );

            entity.setSyncId(parseObject.getObjectId());
            entity.setSyncDate(parseObject.getUpdatedAt());
            entity.setCreationDate(parseObject.getCreatedAt());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends SyncEntity> T fromParseObject(ParseObject parseObject, Class<T> entityClass) {
        try {
            final T entity = entityClass.newInstance();
            fromParseObject(parseObject, entity);
            return entity;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends SyncEntity, P extends ParseObject> void toParseObject(final T entity, final P parseObject) {
        try {
            Class entityClass = entity.getClass();

            // update objectId
            if (entity.getSyncId() != null) {
                Field objectIdField = ParseObject.class.getDeclaredField("objectId");
                objectIdField.setAccessible(true);
                objectIdField.set(parseObject, entity.getSyncId());
                objectIdField.setAccessible(false);
            }

            // update createdAt
            if (entity.getCreationDate() != null) {
                Field createdAtField = ParseObject.class.getDeclaredField("createdAt");
                createdAtField.setAccessible(true);
                createdAtField.set(parseObject, entity.getCreationDate());
                createdAtField.setAccessible(false);
            }

            // update updatedAt
            if (entity.getSyncDate() != null) {
                Field updatedAtField = ParseObject.class.getDeclaredField("updatedAt");
                updatedAtField.setAccessible(true);
                updatedAtField.set(parseObject, entity.getSyncDate());
                updatedAtField.setAccessible(false);
            }

            ReflectionUtils.doWithFields(
                    entityClass,
                    new ReflectionUtils.FieldCallback() {
                        @Override
                        public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                            ParseField a = field.getAnnotation(ParseField.class);
                            if (a == null)
                                return;
                            String parseFieldName = field.getName();
                            if (a.name() != null && !a.name().isEmpty()) {
                                parseFieldName = a.name();
                            }

                            boolean accessible = field.isAccessible();
                            field.setAccessible(true);
                            Object value = field.get(entity);
                            if (value != null)
                                parseObject.put(parseFieldName, value);
                            else parseObject.remove(parseFieldName);
                            field.setAccessible(accessible);
                        }
                    }
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static <T extends SyncEntity> ParseObject toParseObject(T entity) {
        try {
            Class entityClass = entity.getClass();
            ParseClass a = (ParseClass) entityClass.getAnnotation(ParseClass.class);
            String parseClassName = entityClass.getSimpleName();

            if (a.name() != null && !a.name().isEmpty()) {
                parseClassName = a.name();
            }

            ParseObject parseObject = ParseObject.create(parseClassName);
            toParseObject(entity, parseObject);

            return parseObject;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
