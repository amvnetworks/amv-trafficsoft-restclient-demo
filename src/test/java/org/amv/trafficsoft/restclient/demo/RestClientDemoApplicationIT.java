package org.amv.trafficsoft.restclient.demo;

import org.amv.trafficsoft.rest.client.xfcd.XfcdClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = RestClientDemoApplicationIT.TestApplication.class)
public class RestClientDemoApplicationIT {

    @SpringBootApplication
    public static class TestApplication {

    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void contextLoads() {
        final XfcdClient restClient = applicationContext
                .getBean(XfcdClient.class);

        assertThat(restClient, is(notNullValue()));
    }
}
