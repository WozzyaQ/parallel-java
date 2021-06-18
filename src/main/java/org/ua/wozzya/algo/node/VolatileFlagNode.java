package org.ua.wozzya.algo.node;

public class VolatileFlagNode<T> extends VolatileLockableNode<T>{
    private boolean isAlive = true;

    public VolatileFlagNode(T val) {
        super(val);
    }

    public VolatileFlagNode() {
        super();
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }
}
