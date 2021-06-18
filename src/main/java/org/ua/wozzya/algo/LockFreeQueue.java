package org.ua.wozzya.algo;

import org.ua.wozzya.algo.node.VolatileNode;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple queue which implements lock-free {@link LockFreeQueue#put}
 * and {@link LockFreeQueue#poll}
 */
public class LockFreeQueue<T> implements PutPollQueue<T> {
    private final AtomicReference<VolatileNode<T>> head;
    private final AtomicReference<VolatileNode<T>> tail;
    private final AtomicInteger queueSize;


    public LockFreeQueue() {
        head = new AtomicReference<>(null);
        tail = new AtomicReference<>(null);
        queueSize = new AtomicInteger(0);
    }


    @Override
    public void put(T val) {
        Objects.requireNonNull(val, "cannot store nulls");

        VolatileNode<T> newNode = new VolatileNode<>(val);

        // when putting try setting the newNode as queue tail
        // if other thread sets node faster - retry
        VolatileNode<T> tailRef;
        do {
            tailRef = tail.get();
            newNode.setPrev(tailRef);
        } while (!tail.compareAndSet(tailRef, newNode));

        // for backlink
        if (newNode.getPrev() != null) {
            newNode.getPrev().setNext(newNode);
        }

        // for first set of head
        head.compareAndSet(null, newNode);
        queueSize.incrementAndGet();
    }

    @Override
    public Optional<T> poll() {
        if (head.get() == null) {
            return Optional.empty();
        }

        VolatileNode<T> curHeadRef;
        VolatileNode<T> nextToHeadRef;
        // try moving head one node further from current head
        do {
            curHeadRef = head.get();
            nextToHeadRef = curHeadRef.getNext();
        } while (!head.compareAndSet(curHeadRef, nextToHeadRef));

        queueSize.decrementAndGet();
        return Optional.of(curHeadRef.getKey());
    }

    public static void main(String[] args) {
        QueueRunUtils.<Integer>putPollRunInfinite(new LockFreeQueue<>(),
                () -> new Random().nextInt(10),2,500,2000,2);
    }
}
