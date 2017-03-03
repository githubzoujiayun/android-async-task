package com.gplibs.task;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class WaitNotify {

    private final Object mObj;
    private boolean mWait = false;
    private boolean mNotify = false;
    private Lock mLock = new ReentrantLock();

    WaitNotify(Object obj) {
        mObj = obj;
    }

    void doNotify() {
        mLock.lock();
        try {
            mNotify = true;
            if (mWait) {
                synchronized (mObj) {
                    mObj.notify();
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    void doWait() {
        mLock.lock();
        boolean unlock = false;
        try {
            if (mNotify) {
                return;
            }
            mWait = true;
            synchronized (mObj) {
                try {
                    mLock.unlock();
                    unlock = true;
                    mObj.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (!unlock) {
                mLock.unlock();
            }
        }
    }
}
