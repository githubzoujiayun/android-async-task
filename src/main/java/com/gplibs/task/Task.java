package com.gplibs.task;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Task<T> implements Runnable {

    private boolean mSuccess = false;
    private T mResult;
    private Object mTag;
    private Runnable mRunnable;
    private TaskRunnable<T> mTaskRunnable;
    private Throwable mThrowable;

    private Integer mTaskGroup = null;
    private boolean mIsUIThread = false;

    TaskCounter mTaskCounter;

    Task(T result) {
        mSuccess = true;
        mResult = result;
    }

    Task(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException();
        }
        mRunnable = runnable;
    }

    Task(TaskRunnable<T> runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException();
        }
        mTaskRunnable = runnable;
    }

    private Task(final Object obj, final Method method, final Object...params) {
        try {
            method.setAccessible(true);
            mTaskRunnable = new TaskRunnable<T>() {
                @Override
                public T run() throws Throwable {
                    T r = null;
                    Task<T> t = (Task<T>) method.invoke(obj, params);
                    if (t != null) {
                        r = t.getResult();
                    }
                    return r;
                }
            };
            TaskGroup g = method.getAnnotation(TaskGroup.class);
            if (g != null) {
                taskGroup(g.value());
            }
            if (method.getAnnotation(UIThread.class) != null) {
                uiThread(true);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public Task<T> taskGroup(Integer taskGroup) {
        mTaskGroup = taskGroup;
        return this;
    }

    public Integer getTaskGroup() {
        return mTaskGroup;
    }

    public Task<T> uiThread(boolean isUIThread) {
        mIsUIThread = isUIThread;
        return this;
    }

    public boolean isUIThread() {
        return mIsUIThread;
    }

    public Task<T> tag(Object userData) {
        mTag = userData;
        return this;
    }

    public Object getTag() {
        return mTag;
    }

    @Override
    public void run() {
        try {
            if (mTaskRunnable != null) {
                mResult = mTaskRunnable.run();
            } else if (mRunnable != null) {
                mRunnable.run();
            }
            mSuccess = true;
        } catch (Throwable throwable) {
            if (throwable instanceof InvocationTargetException) {
                mThrowable = ((InvocationTargetException) throwable).getTargetException();
            } else {
                mThrowable = throwable;
            }
        }
    }

    public T getResult() {
        return mResult;
    }

    public Throwable getThrowable() {
        return mThrowable;
    }

    public boolean isCompleted() {
        return mSuccess || mThrowable != null;
    }

    public boolean isSuccess() {
        return mSuccess;
    }

    public Task<T> startSync() {
        if (mSuccess) {
            return this;
        } else {
            TaskFactory.startSync(this);
            return this;
        }
    }

    public void startAsync(final TaskCallback<Task<T>> callback) {
        if (mSuccess) {
            doCallback(callback);
        } else {
            TaskEventListener l = new TaskEventListener() {
                @Override
                public void onAllTaskCompleted() {
                    doCallback(callback);
                }

                @Override
                public void onTaskCompleted(Task<?> task) {
                }
            };
            TaskFactory.startAsync(l, this);
        }
    }

    void onComplete() {
        if(mTaskCounter != null) {
            mTaskCounter.onTaskComplete(this);
        }
    }

    private void doCallback(final TaskCallback<Task<T>> callback) {
        if (callback != null) {
            if (callback.mRunOnUIThread) {
                if (!TaskUtils.isUIThread()) {
                    TaskFactory.startAsync(new Task(new Runnable() {
                        @Override
                        public void run() {
                            callback.run(Task.this);
                        }
                    }).uiThread(true));
                } else {
                    callback.run(Task.this);
                }
            } else {
                if (TaskUtils.isUIThread()) {
                    TaskFactory.startAsync(new Task(new Runnable() {
                        @Override
                        public void run() {
                            callback.run(Task.this);
                        }
                    }));
                } else {
                    callback.run(Task.this);
                }
            }
        }
    }
}