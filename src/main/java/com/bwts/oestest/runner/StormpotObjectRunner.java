package com.bwts.oestest.runner;

import com.bwts.oestest.entity.MyObject;
import com.bwts.oestest.entity.MyObjectAllocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;
import stormpot.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Javie on 16/4/11.
 */
public class StormpotObjectRunner {
    static MyObjectAllocator allocator = new MyObjectAllocator();
    static Config<MyObject> config = new Config<>().setAllocator(allocator).setSize(50);
    static Pool<MyObject> pool = new BlazePool<>(config);
    static Timeout timeout = new Timeout(1, TimeUnit.SECONDS);

    private static AtomicLong[] count = new AtomicLong[100];

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        List<Future<AtomicLong>> futureList = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            futureList.add(executorService.submit(new ObjectRunner(now)));
        }
        int i = 0;
        AtomicLong sum = new AtomicLong(0L);
        for (Future<AtomicLong> future : futureList) {
            count[i] = future.get();
            System.out.println("thread " + i + " : " + count[i]);
            sum.addAndGet(count[i].get());
            i++;
        }
        System.out.println("total times : " + sum);
    }

    static class ObjectRunner implements Callable<AtomicLong> {
        private long date;
        private AtomicLong count = new AtomicLong(0L);

        public ObjectRunner(long date) {
            this.date = date;
        }

        @Override
        public AtomicLong call() {

            MyObject myObject = null;
            while (System.currentTimeMillis() < date + 10 * 1000) {
                try {
                    myObject = pool.claim(timeout);
                    while (myObject == null) {
                        myObject = pool.claim(timeout);
                        System.out.println("no usable object!");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
//                    int number = Integer.getInteger(Thread.currentThread().getName().substring(14));
//                    System.out.println(number);
//                    System.out.println(myObject.shout() + count.get() + " thread name: " + Thread.currentThread().getName());
                    count.getAndIncrement();
                } finally {
                    if (myObject != null) {
                        try {
//                            Thread.sleep(30);
                            myObject.release();
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
            return count;
        }
    }
}
