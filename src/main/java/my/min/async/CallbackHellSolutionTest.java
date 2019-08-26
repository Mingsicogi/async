package my.min.async;

import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.function.Consumer;
import java.util.function.Function;

@RestController
public class CallbackHellSolutionTest {

    @Autowired
    private CallbackHellTest.AsyncService asyncService;

    RestTemplate restTemplate = new RestTemplate();
    AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(); // 실제로 내부적으로 스레드를 필요한 만큼 생성시켜 resource 소모가 큼.
    AsyncRestTemplate asyncRestTemplate2 = new AsyncRestTemplate(new Netty4ClientHttpRequestFactory(new NioEventLoopGroup(1)));

    @GetMapping("/solution/callback/hell2")
    public DeferredResult<String> rest(int idx){
        String url = "http://localhost:18083/service1?req={req}";
        String url2 = "http://localhost:18083/service2?req={req}";

        DeferredResult<String> dr = new DeferredResult<>();
        Completion
                .from(asyncRestTemplate2.getForEntity(url, String.class, "hello" + idx))
                .andApply(s -> asyncRestTemplate2.getForEntity(url2, String.class, s.getBody()))
                .andAccept(s -> dr.setResult(s.getBody()));

        return dr;
    }

    @Slf4j
    public static class Completion{

        Completion next;

        static Completion from(ListenableFuture<ResponseEntity<String>> future){
            log.info("##### s:from #####");
            Completion c = new Completion();

            future.addCallback(s -> c.complete(s), e -> c.error(e));

            log.info("##### e:from #####");
            return c;
        }

        private void error(Throwable e) {
            log.error("{}", e.getMessage());
        }

        private void complete(ResponseEntity<String> s) {
            if(next != null){
                log.info("##### complete #####");
                next.run(s);
            }
        }

        private void run(ResponseEntity<String> s) {
            if(consumer != null){
                log.info("##### run:consumer #####");
                consumer.accept(s);

            } else if(fn != null){
                log.info("##### run:fn #####");
                ListenableFuture<ResponseEntity<String>> res = fn.apply(s);
                res.addCallback(s1 -> complete(s1), e1 -> error(e1));
            }
        }

        void andAccept(Consumer<ResponseEntity<String>> consumer) {
            log.info("##### andAccept #####");
            Completion completion = new Completion(consumer);
            this.next = completion;
        }

        Completion andApply(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
            log.info("##### andApply ##### | {}", fn);
            Completion completion = new Completion(fn);
            this.next = completion;

            return completion;
        }

        Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn;
        Completion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) {
            log.info("##### Completion(Function<ResponseEntity<String>, ListenableFuture<ResponseEntity<String>>> fn) #####");
            this.fn = fn;
        }

        Consumer<ResponseEntity<String>> consumer;
        Completion(Consumer<ResponseEntity<String>> consumer){
            log.info("##### Consumer<ResponseEntity<String>> consumer #####");
            this.consumer = consumer;
        }

        Completion(){
            log.info("#####  #####");
        }
    }
}
