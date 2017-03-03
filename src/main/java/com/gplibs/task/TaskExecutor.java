package com.gplibs.task;

import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class TaskExecutor {

    private SparseArray<GroupRunnable> mGroupRunnables;
    private SparseArray<Long> mGroupThreadId;
    private ExecutorService mThreadPool = Executors.newCachedThreadPool();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    TaskExecutor() {
        mGroupRunnables = new SparseArray<>();
        mGroupThreadId = new SparseArray<>();
    }

    synchronized void addTask(Task<?> task) {
        if (task.getTaskGroup() == null) {
            if (task.isUIThread()) {
                runSingleUIThreadTask(task);
            } else {
                runSingleTask(task);
            }
        } else {
            runGroupTask(task);
        }
    }

    private void runSingleTask(final Task<?> task) {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                task.run();
                task.onComplete();
            }
        });
    }

    private void runSingleUIThreadTask(final Task<?> task) {
        if (TaskUtils.isUIThread()) {
            task.run();
            task.onComplete();
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    task.run();
                    task.onComplete();
                }
            });
        }
    }

    private void runGroupTask(Task<?> task) {
        if (!task.isUIThread()) {
            Long tid = mGroupThreadId.get(task.getTaskGroup());
            if(tid != null && TaskUtils.isThread(tid)) {
                task.run();
                task.onComplete();
                return;
            }
        }
        GroupRunnable gr = mGroupRunnables.get(task.getTaskGroup());
        boolean needRun = (gr == null);
        if (needRun) {
            gr = new GroupRunnable(task.getTaskGroup());
            mGroupRunnables.put(task.getTaskGroup(), gr);
        }
        gr.mTasks.add(task);
        if (needRun) {
            run(task.getTaskGroup());
        }
    }

    private void runGroupUIThreadTask(final Task<?> task, final WaitNotify waitNotify) {
        if (TaskUtils.isUIThread()) {
            task.run();
            task.onComplete();
            waitNotify.doNotify();
        } else {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    task.run();
                    task.onComplete();
                    waitNotify.doNotify();
                }
            });
        }
    }

    private void run(int group) {
        GroupRunnable r = mGroupRunnables.get(group);
        if (r != null) {
            mThreadPool.submit(r);
        }
    }

    private class GroupRunnable implements Runnable {

        private int mGroup;
        private CopyOnWriteArrayList<Task<?>> mTasks;

        GroupRunnable(int group) {
            mGroup = group;
            mTasks = new CopyOnWriteArrayList<>();
        }

        public void run() {
            if (mTasks == null) {
                return;
            }
            synchronized (TaskExecutor.this) {
                if (mGroupThreadId.get(mGroup) == null) {
                    mGroupThreadId.put(mGroup, Thread.currentThread().getId());
                }
            }
            while (mTasks.size() > 0) {
                final Task<?> t = mTasks.get(0);
                if (t.isUIThread()) {
                    WaitNotify wn = new WaitNotify(t);
                    runGroupUIThreadTask(t, wn);
                    wn.doWait();
                } else {
                    t.run();
                    t.onComplete();
                }
                mTasks.remove(t);
            }
            synchronized (TaskExecutor.this) {
                if (mTasks.size() > 0) {
                    TaskExecutor.this.run(mGroup);
                    return;
                }
                mGroupRunnables.remove(mGroup);
                mGroupThreadId.remove(mGroup);
            }
        }
    }

}
