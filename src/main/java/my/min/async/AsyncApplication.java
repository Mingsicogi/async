package my.min.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
@Slf4j
@EnableAsync
public class AsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsyncApplication.class, args);
    }

    @RestController
    public static class MyController{

        @GetMapping("/callable")
        public Callable<String> callable(){

            log.info("### Start Callable Method ###");

            return () -> {
                log.info("### Async function start ###");
                Thread.sleep(2000);
                log.info("### Async function end ###");

                return "Hello Callable Function";
            };
        }

        @GetMapping("/async")
        public String async() throws Exception{

            log.info("### Start Simple Async Method ###");

            log.info("### Async function start ###");
            Thread.sleep(2000);
            log.info("### Async function end ###");

            return "Hello Simple Async Function";
        }

        Queue<DeferredResult<String>> resultQueue = new ConcurrentLinkedQueue<>();

        @GetMapping("/dr")
        public DeferredResult<String> df(){
            log.info("### Start DeferredResult ###");

            DeferredResult<String> deferredResult = new DeferredResult<>(60000L);
            resultQueue.add(deferredResult);

            return deferredResult;
        }

        @GetMapping("/dr/count")
        public String drCount(){
            return String.valueOf(resultQueue.size());
        }

        @GetMapping("/dr/event")
        public String drEvent(String msg){
            for(DeferredResult<String> dr : resultQueue){
                dr.setResult("Hello " + msg);
                resultQueue.remove(dr);
            }

            return "OK";
        }

        @GetMapping("/emitter")
        public ResponseBodyEmitter emitter(){
            ResponseBodyEmitter emitter = new ResponseBodyEmitter();

            Executors.newSingleThreadExecutor().submit(() -> {
                for(int i = 0; i < 50; i++){
                    try{
                        emitter.send("<p>Stream " + i + " </p>");
                        Thread.sleep(100);

                    }catch (Exception e){

                    }
                }
            });

            return emitter;
        }
    }

    @Configuration
    public static class AsyncConfig{

        @Bean
        public Executor executor(){
            return Executors.newFixedThreadPool(100);
        }
    }
}
