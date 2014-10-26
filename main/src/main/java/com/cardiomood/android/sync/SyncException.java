package com.cardiomood.android.sync;

/**
 * Created by antondanhsin on 26/10/14.
 */
public class SyncException extends Exception {

    public SyncException(String detailMessage) {
        super(detailMessage);
    }

    public SyncException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public SyncException(Throwable throwable) {
        super(throwable);
    }
}
