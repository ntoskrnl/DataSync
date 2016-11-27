package com.cardiomood.android.sync;

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
