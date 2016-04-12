package com.bwts.oestest.runner;

import com.bwts.oestest.entity.MyObject;
import com.bwts.oestest.utils.Statistics;
import com.haiwanwan.common.objectpool.ObjectFactory;
import com.haiwanwan.common.objectpool.ObjectPool;
import com.haiwanwan.common.objectpool.PoolConfig;
import com.haiwanwan.common.objectpool.Poolable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Javie on 16/4/11.
 */
public class HaiwanwanObjectRunner {

    static ObjectFactory<MyObject> myObjectObjectFactory = new ObjectFactory<MyObject>() {
        @Override
        public MyObject create() {
            return new MyObject();
        }

        @Override
        public void destroy(MyObject myObject) {

        }

        @Override
        public boolean validate(MyObject myObject) {
            return true;
        }
    };

    static PoolConfig config = new PoolConfig();

    static ObjectPool<MyObject> myObjectPool;
    private static AtomicLong[] count = new AtomicLong[100];
    private static List<Long> data = new ArrayList<>();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        config.setPartitionSize(5);
        config.setMaxSize(10);
        config.setMinSize(6);
        config.setMaxIdleMilliseconds(1000 * 3600 * 60 * 8); //repo every 8 hours if idle
        myObjectPool = new ObjectPool<>(config, myObjectObjectFactory);

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
        public AtomicLong call() throws InterruptedException {
            MyObject mo;
            while (System.currentTimeMillis() < date + 60 * 1000) {
                try (Poolable<MyObject> myObject = myObjectPool.borrowObject()) {
                    mo = myObject.getObject();
                    while (mo == null) {
                        mo = myObject.getObject();
                        System.out.println("no usable object!");
                    }
//                    int number = Integer.getInteger(Thread.currentThread().getName().substring(14));
//                    System.out.println(number);
//                    System.out.println(mo.shout() + count.get() + " thread name: " + Thread.currentThread().getName());
                    count.getAndIncrement();
                    Thread.sleep(20);
                }
            }
            return count;
        }
    }
}
