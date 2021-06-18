package org.ua.wozzya.algo;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

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
                    if(ensureConsistency(prev,null)) {
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
                if(!ensureConsistency(prev, cur)) continue onFail;
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
        onFail: while (true) {
            VolatileLockableNode<T> prev = head.get();
            VolatileLockableNode<T> cur = (VolatileLockableNode<T>) prev.getNext();

            while (cur != null && cur.getKey().compareTo(val) < 0) {
                prev = cur;
                cur = (VolatileLockableNode<T>) cur.getNext();
            }
            // if we got end of list
            if(cur == null) {
                // not found
                return false;
            } else {
                prev.lock();
                cur.lock();
                try{
                    if(!ensureConsistency(prev, cur)) continue onFail;
                    if(cur.getKey() == val) {
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

    // TODO more refactoring?
    private boolean ensureConsistency(VolatileLockableNode<T> prev, VolatileLockableNode<T> cur) {
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

            ex.execute(()-> {
                while (true) {
                    int num = new Random().nextInt(10);
                    System.out.println("try remove " + num);
                    boolean res = lst.remove(num);
                    System.out.println("remove " + num + " = " + res);

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
