package org.ua.wozzya.algo;

import java.util.Optional;

public interface PutPollQueue<T> {
    void put(T val);
    Optional<T> poll();
}
