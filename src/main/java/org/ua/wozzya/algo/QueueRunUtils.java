package org.ua.wozzya.algo;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class QueueRunUtils {
    public static <T> void putPollRunInfinite(PutPollQueue<T> queue, Supplier<T> supplier, int putWorkers, int putInterval, int pollWorkerks, int pollInterval) {
        ExecutorService ex = Executors.newFixedThreadPool(3);

        Runnable queuePut = () -> {
            while (true) {
                T val = supplier.get();
                System.out.println("put -> " + val);
                queue.put(val);
                try {
                    Thread.sleep(putInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable queuePoll = () -> {
            while (true) {
                queue.poll().ifPresent(v -> System.out.println("\t\tpoll -> " + v));
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        for (int i = 0; i < putWorkers; ++i) {
            ex.execute(queuePut);
        }

        for (int i = 0; i < pollWorkerks; ++ i) {
            ex.execute(queuePoll);
        }

        ex.shutdown();
    }

}
