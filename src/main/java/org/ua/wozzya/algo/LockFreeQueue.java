package org.ua.wozzya.algo;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple queue which implements lock-free {@link LockFreeQueue#put}
 * and {@link LockFreeQueue#poll}
 */
public class LockFreeQueue<T> implements PutPollQueue<T> {
    private final AtomicReference<AtomicNode<T>> head;
    private final AtomicReference<AtomicNode<T>> tail;
    private final AtomicInteger queueSize;


    public LockFreeQueue() {
        head = new AtomicReference<>(null);
        tail = new AtomicReference<>(null);
        queueSize = new AtomicInteger(0);
    }


    public static class AtomicNode<T> {
        // volatile for granting visibility to all threads
        // when changing next/prev references
        private volatile T key;
        private volatile AtomicNode<T> next;
        private volatile AtomicNode<T> prev;

        public AtomicNode(T key) {
            this.key = key;
            next = null;
            prev = null;
        }

        public AtomicNode<T> getPrev() {
            return prev;
        }

        public void setPrev(AtomicNode<T> prev) {
            this.prev = prev;
        }

        public AtomicNode<T> getNext() {
            return next;
        }

        public void setNext(AtomicNode<T> next) {
            this.next = next;
        }

        public T getKey() {
            return key;
        }

        public void setKey(T key) {
            this.key = key;
        }
    }

    @Override
    public void put(T val) {
        Objects.requireNonNull(val, "cannot store nulls");

        AtomicNode<T> newNode = new AtomicNode<>(val);

        // for first set of head
        head.compareAndSet(null, newNode);

        // when putting try setting the newNode as queue tail
        // if other thread sets node faster - retry
        AtomicNode<T> tailRef;
        do {
            tailRef = tail.get();
            newNode.setPrev(tailRef);
        } while (!tail.compareAndSet(tailRef, newNode));

        // for backlink
        if (newNode.prev != null) {
            newNode.prev.next = newNode;
        }

        queueSize.incrementAndGet();
    }

    @Override
    public Optional<T> poll() {
        if (head.get() == null) {
            return Optional.empty();
        }

        AtomicNode<T> curHeadRef;
        AtomicNode<T> nextToHeadRef;
        // try moving head one node further from current head
        do {
            curHeadRef = head.get();
            nextToHeadRef = curHeadRef.getNext();
        } while (!head.compareAndSet(curHeadRef, nextToHeadRef));

        queueSize.decrementAndGet();
        return Optional.of(curHeadRef.getKey());
    }

    public static void main(String[] args) {
        ExecutorService ex = Executors.newFixedThreadPool(3);
        LockFreeQueue<Integer> queue = new LockFreeQueue<>();

        Runnable queuePut = () -> {
            while (true) {
                int val = new Random().nextInt(10);
                System.out.println("I am putting " + val + " to the queue");
                queue.put(val);
                try {
                    Thread.sleep(900);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable queuePoll = () -> {
            while (true) {
                queue.poll().ifPresent(System.out::println);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        ex.execute(queuePut);
        ex.execute(queuePut);
        ex.execute(queuePoll);

        ex.shutdown();
    }
}
