package org.amv.trafficsoft.restclient.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@Slf4j
@SpringBootApplication
public class RestClientDemoApplication {

    public static void main(String[] args) {
        log.info("Starting {} ...", RestClientDemoApplication.class.getSimpleName());
        new SpringApplicationBuilder(RestClientDemoApplication.class)
                .web(false)
                .bannerMode(Banner.Mode.CONSOLE)
                .run(args);
    }
}
