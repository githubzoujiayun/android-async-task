package com.gplibs.task;

import java.util.concurrent.atomic.AtomicInteger;

class TaskCounter {

    private int mCount = 0;
    private AtomicInteger mCounter = new AtomicInteger(0);
    private WaitNotify waitNotify;
    private TaskEventListener mListener;

    TaskCounter(int count) {
        mCount = count;
        waitNotify = new WaitNotify(this);
    }

    void setListener(TaskEventListener listener) {
        mListener = listener;
    }

    void onTaskComplete(Task<?> task) {
        if (mListener != null) {
            doTaskCompletedCallback(task);
        }
        if(mCounter.addAndGet(1) == mCount) {
            completed();
        }
    }

    void doWait() {
        waitNotify.doWait();
    }

    private void completed() {
        waitNotify.doNotify();
        if (mListener != null) {
            doAllCompletedCallback();
        }
    }

    private void doAllCompletedCallback() {
        if (TaskUtils.isUIThread()) {
            mListener.onAllTaskCompleted();
        } else {
            TaskFactory.startAsync(TaskFactory.newVoidTask(new Runnable() {
                @Override
                public void run() {
                    mListener.onAllTaskCompleted();
                }
            }).uiThread(true));
        }
    }

    private void doTaskCompletedCallback(final Task<?> task) {
        if (TaskUtils.isUIThread()) {
            mListener.onTaskCompleted(task);
        } else {
            TaskFactory.startAsync(TaskFactory.newVoidTask(new Runnable() {
                @Override
                public void run() {
                    mListener.onTaskCompleted(task);
                }
            }).uiThread(true));
        }
    }
}
