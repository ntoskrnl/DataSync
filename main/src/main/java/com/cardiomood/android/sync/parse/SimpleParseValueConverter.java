package com.cardiomood.android.sync.parse;

import java.util.Date;

/**
 * Created by antondanhsin on 18/10/14.
 */
public class SimpleParseValueConverter implements ParseValueConverter {

    @Override
    public <T> T convertValue(Object value, Class<T> targetClass) {
        if (targetClass.isInstance(value)) {
            // no need to convert types
            return (T) value;
        }

        // special handling for null value
        if (value == null) {
            if (targetClass.isPrimitive()) {
                // return 0 or FALSE or null
                if (char.class.equals(targetClass))
                    return (T) new Character('\u0000');
                if (boolean.class.equals(targetClass))
                    return (T) Boolean.FALSE;
                if (byte.class.equals(targetClass))
                    return (T) Byte.valueOf((byte) 0);
                if (short.class.equals(targetClass))
                    return (T) Short.valueOf((short) 0);
                if (int.class.equals(targetClass))
                    return (T) Integer.valueOf(0);
                if (long.class.equals(targetClass))
                    return (T) Long.valueOf(0L);
                if (float.class.equals(targetClass))
                    return (T) Float.valueOf(0.0f);
                if (double.class.equals(targetClass))
                    return (T) Double.valueOf(0.0d);
            }

            // just return null
            return null;
        }

        // ok, value is not null and requires conversion
        if (String.class.equals(targetClass)) {
            // numbers will be converted to String too! :)
            return (T) value.toString();
        }

        if (Date.class.equals(targetClass)) {
            if (Number.class.isInstance(value)) {
                return (T) new Date(((Number) value).longValue());
            }
        }

        if (Long.class.equals(targetClass)) {
            if (Date.class.isInstance(value)) {
                return (T) Long.valueOf((Long) value);
            }
        }

        // target class is not String or Date
        if (Boolean.class.equals(targetClass) || boolean.class.equals(targetClass)) {
            return (T) Boolean.valueOf(value.toString());
        }
        if (Character.class.equals(targetClass) || char.class.equals(targetClass)) {
            if (value.toString().length() == 1) {
                return (T) Character.valueOf(value.toString().charAt(0));
            }
        }
        if (Byte.class.equals(targetClass) || byte.class.equals(targetClass)) {
            return (T) Byte.valueOf(value.toString());
        }
        if (Short.class.equals(targetClass) || short.class.equals(targetClass)) {
            return (T) Short.valueOf(value.toString());
        }
        if (Integer.class.equals(targetClass) || int.class.equals(targetClass)) {
            return (T) Integer.valueOf(value.toString());
        }
        if (Long.class.equals(targetClass) || long.class.equals(targetClass)) {
            return (T) Long.valueOf(value.toString());
        }
        if (Float.class.equals(targetClass) || float.class.equals(targetClass)) {
            return (T) Float.valueOf(value.toString());
        }
        if (Double.class.equals(targetClass) || double.class.equals(targetClass)) {
            return (T) Double.valueOf(value.toString());
        }

        // just return null
        return null;
    }
}
