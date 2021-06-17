package org.ua.wozzya.algo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

public class OptimisticLockingList <T extends Comparable<T>>{
    private AtomicReference<VolatileLockableNode<T>> head;


    public OptimisticLockingList() {
        head = new AtomicReference<>(new VolatileLockableNode<>());
    }

    // TODO refactor this shame (generalize insertion)
    public boolean add(T val) {
        onFail: while (true) {
            VolatileLockableNode<T> prev = head.get();
            VolatileLockableNode<T> node = new VolatileLockableNode<>(val);

            if(prev.getNext() == null) {
                prev.lock();
                if(prev.getNext() == null) {
                    prev.setNext(node);
                    prev.unlock();
                    return true;
                }
                prev.unlock();
            }

            VolatileLockableNode<T> cur = (VolatileLockableNode<T>) prev.getNext();

            while (cur.getKey().compareTo(val) < 0) {
                prev = cur;
                cur = (VolatileLockableNode<T>) cur.getNext();

                // if we have found end of list
                if (cur == null) {
                    prev.lock();
                    if(validate(prev,null)) {
                        prev.setNext(node);
                        prev.unlock();
                        return true;
                    } else {
                        continue onFail;
                    }
                }
            }

            // if we found position to add
            // lock nodes and do validation
            prev.lock();
            cur.lock();
            try {
                if(!validate(prev, cur)) continue onFail;
                if(cur.getKey().compareTo(val) == 0) {
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
        return false;
    }

    private boolean validate(VolatileLockableNode<T> prev, VolatileLockableNode<T> cur) {
        VolatileLockableNode<T> node = head.get();

        while (node != null) {
            if(node == prev) {
                return prev.getNext() == cur;
            }

            // if we nodes contain keys
            // check if we go further than necessary
            if(prev.getKey() != null
                    && node.getKey() != null
                    && node.getKey().compareTo(prev.getKey()) > 0) { break;}

            node = (VolatileLockableNode<T>) node.getNext();
        }
        return false;
    }



    public static void main(String[] args) throws InterruptedException {
        OptimisticLockingList<Integer> lst = new OptimisticLockingList<>();
        int[] ints = new Random().ints(15,-100,100).toArray();

        ExecutorService ex = Executors.newFixedThreadPool(4);
        for(int i = 0; i < ints.length; ++i) {
            int index = i;
            ex.execute(() -> lst.add(ints[index]));
        }

        Thread.sleep(3000);
    }
}
