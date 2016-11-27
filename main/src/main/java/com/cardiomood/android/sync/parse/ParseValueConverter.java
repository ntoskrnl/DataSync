package com.cardiomood.android.sync.parse;

public interface ParseValueConverter {

    public static final ParseValueConverter DEFAULT_VALUE_CONVERTER = new SimpleParseValueConverter();

    <T> T convertValue(Object value, Class<T> targetClass);

}
