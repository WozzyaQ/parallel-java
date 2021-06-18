package org.ua.wozzya.algo;

import org.ua.wozzya.algo.node.VolatileLockableNode;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements find-grained add/remove methods.
 * Fine-grained locking means we locking only those nodes
 * locking which will guarantee list invariant stable (elem < elem + 1)
 *
 * In the case of list, we need to lock the current node and previous one.
 */
public class FineGrainedList<T extends Comparable<T>> {
    private AtomicReference<VolatileLockableNode<T>> head;

    public FineGrainedList() {
        // insert in head pivotal null-node
        head = new AtomicReference<>(new VolatileLockableNode<>());
    }

    public boolean add(T val) {
        // starting from pivotal null-node
        VolatileLockableNode<T> prev = head.get();
        VolatileLockableNode<T> node = new VolatileLockableNode<>(val);

        prev.lock();
        // if list is empty
        if (prev.getNext() == null) {
            prev.setNext(node);
            prev.unlock();
            return true;
        } else {
            VolatileLockableNode<T> cur = (VolatileLockableNode<T>) prev.getNext();
            cur.lock();
            // scroll to desired node
            while (cur.getKey().compareTo(val) < 0) {
                prev.unlock();
                prev = cur;

                cur = (VolatileLockableNode<T>) cur.getNext();

                // if we lucky to find end of list
                // just add new node and return
                if (cur == null) {
                    prev.setNext(node);
                    prev.unlock();
                    return true;
                } else {
                    cur.lock();
                }
            }
            // if the value not in the list
            // link nodes and return
            try {
                if (cur.getKey() == val) {
                    return false;
                } else {
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

    public boolean remove(T val) {
        VolatileLockableNode<T> prev = head.get();

        // case if list is empty
        prev.lock();
        if (prev.getNext() == null) {
            prev.unlock();
            return false;
        }
        VolatileLockableNode<T> cur = (VolatileLockableNode<T>) prev.getNext();
        cur.lock();

        // scroll until we find desired node and unlink it
        // or find end of list
        while (cur != null) {
            T curVal = cur.getKey();
            if (curVal.compareTo(val) == 0) {
                prev.setNext(cur.getNext());
                cur.unlock();
                prev.unlock();
                return true;
            }

            prev.unlock();
            prev = cur;
            cur = (VolatileLockableNode<T>) cur.getNext();

            if (cur != null) {
                cur.lock();
            }
        }

        prev.unlock();
        return false;
    }

    public static void main(String[] args) {
        FineGrainedList<Integer> lst = new FineGrainedList<>();

        int threads = 4;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; ++i) {
            ex.execute(() -> {
                while (true) {
                    Integer val = new Random().nextInt(100);
                    lst.add(val);
                    try {
                        System.out.println(Thread.currentThread().getName() + " add [" + val + "]");
                        Thread.sleep(new Random().nextInt(10));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println(Thread.currentThread().getName() + " remove  [" + (val + 1 )+ "] = " + lst.remove(val +1));
                }
            });
        }
    }
}
