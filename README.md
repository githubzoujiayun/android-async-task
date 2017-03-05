# android-async-task

这是一个Android平台处理复杂异步任务的库

_**1. 安装方法**_

_gradle:_
```Gradle
dependencies {
    compile 'com.gplibs:task:1.0.0'
}
```

_Application onCreate:_
```Java
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        TaskProxy.init(getApplicationContext());
    }
}
```

<br />

---
_**2.  一个简单例子**_

![image](https://github.com/gplibs/resources/raw/master/android/async-task/readme/multi_task_sample.png)

如图 Task1, Task2, Task3 为一组，按先后顺序执行， 其中Task2在主线程执行， Task1, Task3在后台线程执行。

Task4, Task5 为二组，按先后顺序执行，都在后台线程执行。

一组和二组同时启动，都执行完成后，开始Task6(Task6在主线程执行)。

<br />

以下是使用该库实现上面过程的代码：

_TasksSample:_
```Java
public class TasksSample extends TaskProxy<TasksSample> {

    @TaskGroup(1)
    protected Task<Void> task1() {
        Log.d("TASK", "Task1 begin - isUIThread:" + isUIThread());
        sleep(1000);
        Log.d("TASK", "Task1 end");
        return VoidTask();
    }

    @UIThread
    @TaskGroup(1)
    protected Task<Void> task2() {
        Log.d("TASK", "Task2 begin - isUIThread:" + isUIThread());
        sleep(1000);
        Log.d("TASK", "Task2 end");
        return VoidTask();
    }

    @TaskGroup(1)
    protected Task<Void> task3() {
        Log.d("TASK", "Task3 begin - isUIThread:" + isUIThread());
        sleep(1000);
        Log.d("TASK", "Task3 end");
        return VoidTask();
    }

    @TaskGroup(2)
    protected Task<Void> task4() {
        Log.d("TASK", "Task4 begin - isUIThread:" + isUIThread());
        sleep(500);
        Log.d("TASK", "Task4 end");
        return VoidTask();
    }

    @TaskGroup(2)
    protected Task<Void> task5() {
        Log.d("TASK", "Task5 begin - isUIThread:" + isUIThread());
        sleep(2500);
        Log.d("TASK", "Task5 end");
        return VoidTask();
    }

    @UIThread
    protected Task<Void> task6() {
        Log.d("TASK", "Task6 begin - isUIThread:" + isUIThread());
        sleep(100);
        Log.d("TASK", "Task6 end");
        return VoidTask();
    }

    public Task<Void> doTasks() {
        Log.d("TASK", "TasksSample tasks begin");
        TaskFactory.startSync(
                getProxy().task1().tag(1),
                getProxy().task2().tag(2),
                getProxy().task3().tag(3),
                getProxy().task4().tag(4),
                getProxy().task5().tag(5)
        );
        getProxy().task6().tag(6).startSync();
        Log.d("TASK", "TasksSample tasks end");
        return VoidTask();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isUIThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }
}
```

_调用 TasksSample:_
```Java
public class TasksActivity extends AppCompatActivity {

    TasksSample mSample = new TasksSample();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        test();
    }

    private void test() {
        Log.d("TASK", "Activity test begin");
        mSample.getProxy().doTasks().startAsync(new TaskCallback<Task<Void>>() {
            @Override
            public void run(Task<Void> task) {
                Log.d("TASK", "doTasks callback");
            }
        });
        Log.d("TASK", "Activity test end");
    }
}
```

_运行结果:_

![image](https://github.com/gplibs/resources/raw/master/android/async-task/readme/multi_task_sample_result.png)

如上 书写起来非常简洁方便；

@TaskGroup 给任务分组， @UIThread 标识在主线程执行； 

多个分组的任务直接丢进 TaskFactory 内部自动根据注解执行（同一组的任务按放进的先后顺序执行）。

<br />

---
_**3.  有返回值的任务**_

_ResultTaskSample:_
```Java
public class ResultTaskSample extends TaskProxy<ResultTaskSample> {

    public Task<String> resultTask() {
        sleep(1000);
        return Task("string result");
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
```

_调用 ResultTaskSample:_

```Java
ResultTaskSample sample = new ResultTaskSample();

...

// 同步调用（会阻塞调用线程）
Task<String> task = sample.getProxy().resultTask().startSync();
String result = task.getResult();
Log.d("TASK", "result is:" + result);

...

// 异步调用
sample.getProxy().resultTask().startAsync(new TaskCallback<Task<String>>() {
    @Override
    public void run(Task<String> task) {
        String result = task.getResult();
        Log.d("TASK", "result is:" + result);
    }
});

```

<br />

---
_**4.  处理任务中发生的异常**_

```Java
ResultTaskSample sample = new ResultTaskSample();

...

// 同步调用（会阻塞调用线程）
Task<String> task = sample.getProxy().resultTask().startSync();
if (task.isSuccess()) {
    String result = task.getResult();
    Log.d("TASK", "result is:" + result);
} else {
    // 打印异常信息
    task.getThrowable().printStackTrace();
}

...

// 异步调用
sample.getProxy().resultTask().startAsync(new TaskCallback<Task<String>>() {
    @Override
    public void run(Task<String> task) {
        if (task.isSuccess()) {
            String result = task.getResult();
            Log.d("TASK", "result is:" + result);
        } else {
            // 打印异常信息
            task.getThrowable().printStackTrace();
        }
    }
});

```

<br />

---
_**5.  多任务事件**_

当用TaskFactory启动多个任务时， 可以通过 TaskEventListener 获知各 Task 的执行情况。

_TaskEventSample:_
```Java
public class TaskEventSample extends TaskProxy<TaskEventSample> {

    protected Task<Void> task1() {
        Log.d("TASK", "Task1 begin");
        sleep(500);
        Log.d("TASK", "Task1 end");
        return VoidTask();
    }

    protected Task<Void> task2() {
        Log.d("TASK", "Task2 begin");
        sleep(1000);
        Log.d("TASK", "Task2 end");
        return VoidTask();
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
```

_调用 TaskEventSample:_

```Java
TaskEventSample sample = new TaskEventSample();

...

TaskEventListener listener = new TaskEventListener() {
    @Override
    public void onAllTaskCompleted() {
        Log.d("TASK", "all task completed");
    }
    @Override
    public void onTaskCompleted(Task<?> task) {
        Log.d("TASK", "A task completed, tag is:" + task.getTag());
    }
};
TaskFactory.startAsync(
        listener,
        sample.getProxy().task1().tag("1"),
        sample.getProxy().task2().tag("2")
);
```