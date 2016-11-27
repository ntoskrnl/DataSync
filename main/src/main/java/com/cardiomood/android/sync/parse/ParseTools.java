package com.cardiomood.android.sync.parse;

import android.text.TextUtils;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;

public final class ParseTools {

    private ParseTools() {
        // don't instantiate this!
    }

    public static final int DEFAULT_PARSE_QUERY_LIMIT = 100;

    public static String getUserFullName(ParseUser pu) {
        String fullName = pu.has("lastName") ? pu.getString("lastName") : "";
        if (!TextUtils.isEmpty(fullName))
            fullName += " ";
        if (pu.has("firstName"))
            fullName += pu.getString("firstName");
        return fullName;
    }

    public static <T extends ParseObject> Task<List<T>> findAllParseObjectsAsync(Class<T> clazz) {
        return findAllParseObjectsAsync(ParseQuery.getQuery(clazz));
    }

    public static <T extends ParseObject> Task<List<T>> findAllParseObjectsAsync(final ParseQuery<T> query) {
        return Task.callInBackground(new Callable<List<T>>() {
            @Override
            public List<T> call() throws Exception {
                return findAllParseObjects(query);
            }
        });
    }

    public static <T extends ParseObject> List<T> findAllParseObjects(Class<T> clazz) throws ParseException {
        return findAllParseObjects(ParseQuery.getQuery(clazz));
    }

    public static <T extends ParseObject> List<T> findAllParseObjects(ParseQuery<T> query) throws ParseException {
        List <T> result = new ArrayList<T>();
        query.setLimit(DEFAULT_PARSE_QUERY_LIMIT);
        List<T> chunk = null;
        do {
            chunk = query.find();
            result.addAll(chunk);
            query.setSkip(query.getSkip() + query.getLimit());
        } while (chunk.size() == query.getLimit());
        return result;
    }
}
