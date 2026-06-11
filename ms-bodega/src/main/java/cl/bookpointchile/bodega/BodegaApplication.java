package cl.bookpointchile.bodega;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BodegaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BodegaApplication.class, args);
    }
}
