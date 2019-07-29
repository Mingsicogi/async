package my.min.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class PerformanceTest {
    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException{
        ExecutorService es = Executors.newFixedThreadPool(100);

        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:18082/dr";

        StopWatch mainStopwatch = new StopWatch();
        mainStopwatch.start();

        for(int i = 0; i < 100; i++){
            es.execute(() -> {
                int idx = counter.addAndGet(1);

                StopWatch sw = new StopWatch();
                sw.start();
                restTemplate.getForObject(url, String.class);
                sw.stop();

                log.info("Thread {} start | It tooks {} seconds", idx, sw.getTotalTimeSeconds());

            });
        }

        es.shutdown();
        es.awaitTermination(1000, TimeUnit.SECONDS);

        mainStopwatch.stop();
        log.info("### Total working time : {} seconds ###", mainStopwatch.getTotalTimeSeconds());
    }
}
