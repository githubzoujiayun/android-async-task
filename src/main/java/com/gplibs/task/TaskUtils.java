package com.gplibs.task;

import android.os.Looper;

class TaskUtils {

    static boolean isSubClass(Class subClass, Class parentClass) {
        if (subClass == null || parentClass == null) {
            return false;
        }

        if (subClass.equals(parentClass)) {
            return true;
        }

        if (isSubClass(subClass.getSuperclass(), parentClass)) {
            return true;
        }

        Class[] is = subClass.getInterfaces();
        for (Class i : is) {
            if (isSubClass(i, parentClass)) {
                return true;
            }
        }

        return false;
    }

    static boolean isThread(long threadId) {
        return Thread.currentThread().getId() == threadId;
    }

    static boolean isUIThread() {
        return isThread(Looper.getMainLooper().getThread().getId());
    }
}
