package my.min.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PerformanceTest {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:18082/solution/callback/hell2?idx={idx}";

        CyclicBarrier cyclicBarrier = new CyclicBarrier(2);// 스레드 동기화! 101째

        for(int i = 0; i < 1; i++){
            es.submit(() -> {
                int idx = counter.addAndGet(1);

                cyclicBarrier.await();

                StopWatch sw = new StopWatch();
                sw.start();

                log.info("### Thread {} start ###", idx);
                String res = restTemplate.getForObject(url, String.class, idx);

                sw.stop();

                log.info("{} Thread | It tooks {} seconds | Response : {}", idx, sw.getTotalTimeSeconds(), res);

                return null;
            });
        }

        cyclicBarrier.await();

        StopWatch mainStopwatch = new StopWatch();
        mainStopwatch.start();

        es.shutdown();
        es.awaitTermination(1000, TimeUnit.SECONDS);

        mainStopwatch.stop();
        log.info("### Total working time : {} seconds ###", mainStopwatch.getTotalTimeSeconds());
    }
}
