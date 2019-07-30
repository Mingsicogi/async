package my.min.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RemoteServerTest {

    @RestController
    public class MyController {

        @GetMapping("/service1")
        public String service1(String req) throws InterruptedException {
            Thread.sleep(1000);
            return "service1_" + req;
        }

        @GetMapping("/service2")
        public String service2(String req) throws InterruptedException {
            Thread.sleep(1000);
            return "service2_" + req;
        }
    }

    public static void main(String[] args) {
        System.setProperty("server.port", "18083");
        System.setProperty("server.tomcat.max-threads", "100");
        SpringApplication.run(RemoteServerTest.class, args);
    }
}
