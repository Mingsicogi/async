package my.min.async;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
public class CallbackHellTest {

    @Autowired
    private AsyncService asyncService;

    RestTemplate restTemplate = new RestTemplate();
    AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(); // 실제로 내부적으로 스레드를 필요한 만큼 생성시켜 resource 소모가 큼.
    AsyncRestTemplate asyncRestTemplate2 = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @GetMapping("/rest")
    public DeferredResult<String> rest(int idx){
        String url = "http://localhost:18083/service1?req={req}";
        String url2 = "http://localhost:18083/service2?req={req}";

//        String res = restTemplate.getForObject(url, String.class, "hello" + idx);

//        ListenableFuture<ResponseEntity<String>> res = asyncRestTemplate.getForEntity(url, String.class, "hello" + idx);

        ListenableFuture<ResponseEntity<String>> res = asyncRestTemplate2.getForEntity(url, String.class, "hello" + idx);
        DeferredResult<String> dr = new DeferredResult<>();

        res.addCallback( s -> {
            ListenableFuture<ResponseEntity<String>> res2 = asyncRestTemplate2.getForEntity(url2, String.class, s.getBody());

            res2.addCallback(s2 -> {

                ListenableFuture<String> res3 = asyncService.work(s2.getBody());
                res3.addCallback(s3 -> dr.setResult(s3), e -> dr.setErrorResult(e.getMessage()));

            }, e -> dr.setErrorResult(e.getMessage()));

        }, e -> dr.setErrorResult(e.getMessage()));

        return dr;
    }

    @Service
    public static class AsyncService{

        @Async
        public ListenableFuture<String> work(String req){
            return new AsyncResult<>(req + "_asyncWorking");
        }
    }

    @Bean
    public ThreadPoolTaskExecutor myThreadPool(){

        // 자바의 스레드풀 동작은 큐를 먼저 채우고, 큐까지 다채워졌을때 maxpool 사이즈 만큼 다시 채움
        ThreadPoolTaskExecutor te = new ThreadPoolTaskExecutor();
        te.setCorePoolSize(1);
        te.setMaxPoolSize(1);
        te.initialize();

        return te;
    }
}
