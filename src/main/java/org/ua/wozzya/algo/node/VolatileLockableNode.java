package org.ua.wozzya.algo.node;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VolatileLockableNode<T> extends VolatileNode<T> {

    private final Lock lock;

    public VolatileLockableNode() {
        this(null);
    }

    public VolatileLockableNode(T key) {
        super(key);
        lock = new ReentrantLock();
    }

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }
}
