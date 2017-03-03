package com.gplibs.task;

abstract class ResultCallback<T> {
    boolean mRunOnUIThread;

    public ResultCallback(boolean runOnUIThread) {
        mRunOnUIThread = runOnUIThread;
    }

    public ResultCallback() {
        this(true);
    }

    public abstract void run(T result);
}
