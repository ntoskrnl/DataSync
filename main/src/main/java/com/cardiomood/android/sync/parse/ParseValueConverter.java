package com.cardiomood.android.sync.parse;

/**
 * Created by antondanhsin on 18/10/14.
 */
public interface ParseValueConverter {

    public static final ParseValueConverter DEFAULT_VALUE_CONVERTER = new SimpleParseValueConverter();

    <T> T convertValue(Object value, Class<T> targetClass);

}
