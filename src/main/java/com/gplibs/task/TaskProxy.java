package com.gplibs.task;

import android.content.Context;

import com.android.dx.stock.ProxyBuilder;

import java.io.File;

public abstract class TaskProxy<T extends TaskProxy<?>> {

    private static File sCacheDir;

    private TaskProxy mProxy;
    private WaitNotify mWaitNotify;

    public TaskProxy() {
        if (!ProxyBuilder.isProxyClass(this.getClass())) {
            mWaitNotify = new WaitNotify(this);
            initProxy();
        }
    }

    public synchronized static void init(Context context) {
        initCacheDir(context);
    }

    private static void initCacheDir(Context context) {
        if (sCacheDir == null) {
            sCacheDir = new File(context.getCacheDir().getAbsolutePath() + "/pxy_cache/");
        }
        if (!sCacheDir.exists()) {
            sCacheDir.mkdir();
        }
    }

    private void initProxy() {
        TaskFactory.startAsync(new Task(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (TaskProxy.class) {
                        mProxy = ProxyBuilder.forClass(TaskProxy.this.getClass())
                                .dexCache(sCacheDir)
                                .parentClassLoader(TaskProxy.this.getClass().getClassLoader())
                                .handler(new TaskInvocationHandler(TaskProxy.this))
                                .build();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    mWaitNotify.doNotify();
                }
            }
        }));
    }

    public T getProxy() {
        if (mProxy != null) {
            return (T) mProxy;
        } else {
            mWaitNotify.doWait();
            try {
                return (T) mProxy;
            } catch (Throwable t) {
                t.printStackTrace();
                return null;
            }
        }
    }

    @ProxyIgnore
    protected <TResult> Task<TResult> Task(TResult result) {
        return new Task<TResult>(result);
    }

    @ProxyIgnore
    protected Task<Void> VoidTask() {
        return Task(null);
    }
}
