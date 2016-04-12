package com.bwts.oestest;

import com.bwts.oestest.service.EstampService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan
public class OESTestRunner {

//    @Autowired
//    public static EstampService estampService;

    private static AtomicLong count = new AtomicLong(0L);

    static class OESRunner implements Runnable {

        public OESRunner() {
        }

        @Override
        public void run() {
            while (true) {
                EstampService estampService = new EstampService();
                EstampService.BAWrapper sig = estampService.getImage("0000000001000001");
                if (sig != null)
                    count.getAndIncrement();
                System.out.println(sig != null ? sig.getData().length + "   count:" + count.get() : "null");
            }
        }
    }

    public static void main(String[] args) {
        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < 10; i++) {
            es.submit(new OESRunner());
        }
    }
}
