package com.bwts.oestest.runner;

import com.bwts.oestest.entity.MyObject;
import com.bwts.oestest.utils.Statistics;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Javie on 16/4/12.
 */
public class ApacheObjectRunner {
    private static GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    static {
        config.setMaxTotal(50);
        config.setBlockWhenExhausted(true);
        config.setFairness(true);
    }
    private static class MyObjectPoolFactory extends BasePooledObjectFactory<MyObject> {
        @Override
        public MyObject create() throws Exception {
            return new MyObject();
        }

        @Override
        public PooledObject<MyObject> wrap(MyObject obj) {
            return new DefaultPooledObject<>(obj);
        }
    }
    private static ObjectPool<MyObject> pool = new GenericObjectPool<>(new MyObjectPoolFactory(), config);
    private static AtomicLong[] count = new AtomicLong[100];
    private static List<Long> data = new ArrayList<>();

    public static void main(String[] args) throws ExecutionException, InterruptedException {
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
            MyObject myObject = null;
            while (System.currentTimeMillis() < date + 60 * 1000) {
                try {
                    myObject = pool.borrowObject();
                    while (myObject == null) {
                        myObject = pool.borrowObject();
                        System.out.println("no usable object!");
                    }
//                    int number = Integer.getInteger(Thread.currentThread().getName().substring(14));
//                    System.out.println(number);
//                    System.out.println(mo.shout() + count.get() + " thread name: " + Thread.currentThread().getName());
                    count.getAndIncrement();
                    Thread.sleep(20);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (myObject != null) {
                        try {
                            pool.returnObject(myObject);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return count;
        }
    }
}
