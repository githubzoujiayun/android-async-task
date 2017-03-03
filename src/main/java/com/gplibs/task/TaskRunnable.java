package com.gplibs.task;

public interface TaskRunnable<T> {
    T run() throws Throwable;
}
