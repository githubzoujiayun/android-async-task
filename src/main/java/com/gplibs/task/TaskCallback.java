package com.gplibs.task;

public abstract class TaskCallback<T extends Task<?>> extends ResultCallback<T> {

    public TaskCallback(boolean runOnUIThread){
        super(runOnUIThread);
    }

    public TaskCallback() {
        super();
    }

    @Override
    public abstract void run(T task);
}
