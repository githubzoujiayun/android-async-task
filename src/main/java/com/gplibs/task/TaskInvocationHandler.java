package com.gplibs.task;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class TaskInvocationHandler implements InvocationHandler {

    private Object mObject;

    TaskInvocationHandler(Object object) {
        mObject = object;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        method.setAccessible(true);
        Class<?> t = method.getReturnType();
        boolean isTaskMethod = TaskUtils.isSubClass(t, Task.class);
        boolean proxyIgnore = false;
        if (isTaskMethod) {
            proxyIgnore = method.getAnnotation(ProxyIgnore.class) != null;
        }
        if (isTaskMethod && !proxyIgnore) {
            try {
                Constructor c = t.getDeclaredConstructor(Object.class, Method.class, Object[].class);
                c.setAccessible(true);
                return c.newInstance(mObject, method, args);
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return method.invoke(mObject, args);
    }
}
