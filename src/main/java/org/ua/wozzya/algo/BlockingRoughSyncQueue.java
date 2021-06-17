package org.ua.wozzya.algo;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockingRoughSyncQueue<T> implements PutPollQueue<T> {

    private final AtomicReference<VolatileNode<T>> head;
    private final AtomicReference<VolatileNode<T>> tail;
    private final Lock lock;

    public BlockingRoughSyncQueue() {
        head = new AtomicReference<>(null);
        tail = new AtomicReference<>(null);
        lock = new ReentrantLock();
    }

    @Override
    public void put(T val) {
        lock.lock();
        putUnsafe(val);
        lock.unlock();
    }

    public void putUnsafe(T val) {
        VolatileNode<T> newTail = new VolatileNode<>(val);
        // for insertion when empty
        head.compareAndSet(null, newTail);

        VolatileNode<T> curTail = tail.get();
        newTail.setPrev(curTail);

        tail.compareAndSet(curTail, newTail);

        if (newTail.getPrev() != null) {
            newTail.getPrev().setNext(newTail);
        }
    }

    @Override
    public Optional<T> poll() {
        lock.lock();
        Optional<T> polled = pollUnsafe();
        lock.unlock();
        return polled;
    }

    private Optional<T> pollUnsafe() {
        if (head.get() == null) {
            return Optional.empty();
        }

        VolatileNode<T> currentHead = head.get();
        VolatileNode<T> nextToHead = currentHead.getNext();
        head.compareAndSet(currentHead, nextToHead);

        if (nextToHead != null) {
            nextToHead.setPrev(null);
        }

        return Optional.of(currentHead.getKey());
    }

    public static void main(String[] args) {
        QueueRunUtils.putPollRunInfinite(new BlockingRoughSyncQueue<>(),
                () -> new Random().nextGaussian(), 2,100,2,100);
    }
}
