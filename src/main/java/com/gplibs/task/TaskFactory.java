package com.gplibs.task;

public class TaskFactory {

    private static TaskExecutor mTaskExecutor = new TaskExecutor();

    public static Task<Void> newVoidTask(Runnable runnable) {
        return new Task<Void>(runnable);
    }

    public static <T> Task<T> newTask(TaskRunnable<T> taskRunnable) {
        return new Task<T>(taskRunnable);
    }

    public static void startAsync(Task<?>... tasks) {
        startAsync((TaskEventListener) null, tasks);
    }

    public static void startAsync(TaskEventListener listener, Task<?>... tasks) {
        startTasks(listener, false, tasks);
    }

    public static void startSync(Task<?>... tasks) {
        startSync((TaskEventListener) null, tasks);
    }

    public static void startSync(TaskEventListener listener, Task<?>... tasks) {
        startTasks(listener, true, tasks);
    }

    public static void startTasks(TaskEventListener listener, boolean wait, Task<?>... tasks) {
        if (tasks == null || tasks.length == 0) {
            throw new IllegalArgumentException();
        }
        TaskCounter c = new TaskCounter(tasks.length);
        if (listener != null) {
            c.setListener(listener);
        }
        for (Task<?> t : tasks) {
            if (t != null) {
                t.mTaskCounter = c;
                mTaskExecutor.addTask(t);
            } else {
                c.onTaskComplete(null);
            }
        }
        if (wait) {
            c.doWait();
        }
    }

}
