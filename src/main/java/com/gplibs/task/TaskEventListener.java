package com.gplibs.task;

public interface TaskEventListener {
    void onAllTaskCompleted();
    void onTaskCompleted(Task<?> task);
}
