package org.ua.wozzya.algo;

public class VolatileNode<T> {
    // volatile for granting visibility to all threads
    // when changing next/prev references
    private volatile T key;
    private volatile VolatileNode<T> next;
    private volatile VolatileNode<T> prev;

    public VolatileNode(T key) {
        this.key = key;
        next = null;
        prev = null;
    }

    public VolatileNode<T> getPrev() {
        return prev;
    }

    public void setPrev(VolatileNode<T> prev) {
        this.prev = prev;
    }

    public VolatileNode<T> getNext() {
        return next;
    }

    public void setNext(VolatileNode<T> next) {
        this.next = next;
    }

    public T getKey() {
        return key;
    }

    public void setKey(T key) {
        this.key = key;
    }
}