package com.bwts.oestest.runner;

import com.bwts.oestest.entity.MyObject;
import com.bwts.oestest.utils.Statistics;
import stormpot.Allocator;
import stormpot.BlazePool;
import stormpot.Config;
import stormpot.Pool;
import stormpot.Slot;
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
    static class MyObjectAllocator implements Allocator<MyObject> {
        @Override
        public MyObject allocate(Slot slot) throws Exception {
            return new MyObject(slot);
        }

        @Override
        public void deallocate(MyObject poolable) throws Exception {

        }
    }

    static Config<MyObject> config = new Config<>().setAllocator(new MyObjectAllocator()).setSize(50);
    static Pool<MyObject> pool = new BlazePool<>(config);
    static Timeout timeout = new Timeout(3, TimeUnit.MINUTES);

    private static AtomicLong[] count = new AtomicLong[100];
    private static List<Long> data = new ArrayList<>();

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
            data.add(count[i].get());
            i++;
        }
        System.out.println("total times : " + sum);
        System.out.println("variance: " + Statistics.getVariance(data));
        System.out.println("range: " + Statistics.getRange(data));
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
            while (System.currentTimeMillis() < date + 60 * 1000) {
                try {
                    myObject = pool.claim(timeout);
//                    while (myObject == null) {
//                        myObject = pool.claim(timeout);
//                        System.out.println("no usable object!");
//                    }
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
                            Thread.sleep(20);
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
