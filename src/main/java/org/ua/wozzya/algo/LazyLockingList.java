package org.ua.wozzya.algo;

import org.ua.wozzya.algo.node.VolatileFlagNode;
import org.ua.wozzya.algo.node.VolatileLockableNode;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements lazy-locking add/remove methods
 *
 * Instead of traversing to nodes and ensuring order consistency
 * we check isAlive flags after locking previous and current nodes
 */
public class LazyLockingList<T extends Comparable<T>> {
    private final AtomicReference<VolatileFlagNode<T>> head;

    public LazyLockingList() {
        head = new AtomicReference<>(new VolatileFlagNode<>());
    }

    private boolean add(T val) {
        onFail:
        while (true) {
            VolatileFlagNode<T> prev = head.get();
            VolatileFlagNode<T> cur = (VolatileFlagNode<T>) prev.getNext();

            while (cur != null && cur.getKey().compareTo(val) < 0) {
                prev = cur;
                cur = (VolatileFlagNode<T>) cur.getNext();
            }

            // case - adding in end of list

            if (cur == null) {
                try {
                    prev.lock();
                    if (ensureConsistency(prev, null)) {
                        prev.setNext(new VolatileFlagNode<>(val));
                        return true;
                    } else {
                        continue onFail;
                    }
                } finally {
                    prev.unlock();
                }
            }

            // other cases
            try {
                prev.lock();
                cur.lock();
                if (!ensureConsistency(prev, cur)) continue onFail;

                if (cur.getKey() == val) {
                    return false;
                } else {
                    VolatileFlagNode<T> node = new VolatileFlagNode<>(val);
                    node.setNext(cur);
                    prev.setNext(node);
                    return true;
                }
            } finally {
                cur.unlock();
                prev.unlock();
            }

        }
    }

    private boolean remove(T val) {
        onFail: while (true) {
            VolatileFlagNode<T> prev = head.get();
            VolatileFlagNode<T> cur = (VolatileFlagNode<T>) prev.getNext();

            while (cur != null && cur.getKey().compareTo(val) < 0) {
                prev = cur;
                cur = (VolatileFlagNode<T>) cur.getNext();
            }

            // end of list
            if(cur == null) {
                return false;
            } else {
                prev.lock();
                cur.lock();
                try {
                    if(!ensureConsistency(prev, cur)) continue onFail;
                    if(cur.getKey() == val) {
                        // marking current as dead
                        cur.setAlive(false);
                        prev.setNext(cur.getNext());
                        return true;
                    } else {
                        return false;
                    }
                } finally {
                    cur.unlock();
                    prev.unlock();
                }
            }
        }
    }

    private boolean ensureConsistency(VolatileFlagNode<T> prev, VolatileFlagNode<T> cur) {
        // case adding in list's tail
        if (cur == null) {
            return prev.isAlive();
        }
        return prev.isAlive() && cur.isAlive() && prev.getNext() == cur;
    }

    public static void main(String[] args) {
        LazyLockingList<Integer> lst = new LazyLockingList<>();
        int numOfThreads = 4;
        ExecutorService ex = Executors.newFixedThreadPool(numOfThreads);
        for(int i = 0; i < numOfThreads; ++i) {
            ex.execute(() -> {
                while (true) {
                    int num = new Random().nextInt(10);
                    System.out.println("try add " + num);
                    boolean res = lst.add(num);
                    System.out.println("add " + num + " = " + res);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            ex.execute(() -> {
                while (true) {
                    int num = new Random().nextInt(10);
                    System.out.println("\t\ttry remove " + num);
                    boolean res = lst.remove(num);
                    System.out.println("\t\tremove " + num + " = " + res);

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
