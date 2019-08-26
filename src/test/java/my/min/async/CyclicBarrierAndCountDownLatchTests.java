package my.min.async;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@Slf4j
public class CyclicBarrierAndCountDownLatchTests {

    @Test
    public void countDownLatchTest() {
        CountDownLatch countDownLatch = new CountDownLatch(7);
        ExecutorService es = Executors.newFixedThreadPool(7);
        List<String> outputScraper = new ArrayList<>();
        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            es.execute(() -> {
                if (countDownLatch.getCount() != 0) {
                    outputScraper.add("Count Updated");
                }
                countDownLatch.countDown();

                try {
                    countDownLatch.await();
//                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                log.info("Thread : {} | i : {}", Thread.currentThread().getName(), counter.addAndGet(1));
            });
        }
        es.shutdown();

        counter.intValue();
        outputScraper.forEach(s -> {
            System.out.println(counter.addAndGet(1) + " : " + s);
        });

        assertTrue(outputScraper.size() <= 7);
    }

    @Test
    public void cyclicBarrierTest(){
        CyclicBarrier cyclicBarrier = new CyclicBarrier(7);
        List<String> outputScraper = new ArrayList<>();
        ExecutorService es = Executors.newFixedThreadPool(20);
        final AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 20; i++) {
            es.execute(() -> {
                try {
                    if (cyclicBarrier.getNumberWaiting() <= 0) {
                        outputScraper.add("Count Updated");
                    }
                    cyclicBarrier.await();

                    log.info("Thread : {} | i : {}", Thread.currentThread().getName(), counter.addAndGet(1));
                } catch (InterruptedException | BrokenBarrierException e) {
                    // error handling
                }
            });
        }
        es.shutdown();

        counter.intValue();
        outputScraper.forEach(s -> {
            System.out.println(counter.addAndGet(1) + " : " + s);
        });

        assertTrue(outputScraper.size() > 7);
    }
}
