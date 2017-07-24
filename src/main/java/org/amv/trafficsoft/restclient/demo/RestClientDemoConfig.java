package org.amv.trafficsoft.restclient.demo;

import com.netflix.hystrix.*;
import feign.Feign;
import feign.hystrix.SetterFactory;
import org.amv.trafficsoft.rest.client.asgregister.AsgRegisterClient;
import org.amv.trafficsoft.rest.client.autoconfigure.TrafficsoftApiRestProperties;
import org.amv.trafficsoft.rest.client.xfcd.XfcdClient;
import org.amv.trafficsoft.restclient.demo.command.AllSeriesAndModelsOfOemRunner;
import org.amv.trafficsoft.restclient.demo.command.GetDataAndConfirmDeliveriesRecursiveRunner;
import org.amv.trafficsoft.restclient.demo.command.GetDataAndConfirmDeliveriesRunner;
import org.amv.trafficsoft.restclient.demo.command.LastDataRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@EnableConfigurationProperties(RestClientDemoProperties.class)
public class RestClientDemoConfig {

    private final TrafficsoftApiRestProperties trafficsoftApiRestProperties;
    private final RestClientDemoProperties restClientDemoProperties;

    @Autowired
    public RestClientDemoConfig(TrafficsoftApiRestProperties trafficsoftApiRestProperties,
                                RestClientDemoProperties restClientDemoProperties) {
        this.trafficsoftApiRestProperties = requireNonNull(trafficsoftApiRestProperties);
        this.restClientDemoProperties = requireNonNull(restClientDemoProperties);
    }

    @Bean
    @Profile("get-last-data-demo")
    public CommandLineRunner lastDataRunner(XfcdClient xfcdClient) {
        return new LastDataRunner(
                xfcdClient,
                trafficsoftApiRestProperties.getContractId(),
                restClientDemoProperties.getVehicleIds()
        );
    }

    @Bean
    @Profile("get-oems-demo")
    public CommandLineRunner allSeriesAndModelsOfOemRunner(AsgRegisterClient asgRegisterClient) {
        return new AllSeriesAndModelsOfOemRunner(
                asgRegisterClient,
                trafficsoftApiRestProperties.getContractId()
        );
    }

    @Bean
    @Profile("get-data-and-confirm-deliveries-demo")
    public CommandLineRunner getDataAndConfirmDeliveriesRunner(XfcdClient xfcdClient) {
        return new GetDataAndConfirmDeliveriesRunner(
                xfcdClient,
                trafficsoftApiRestProperties.getContractId()
        );
    }

    @Bean
    @Profile("get-data-and-confirm-deliveries-recursive-demo")
    public CommandLineRunner getDataAndConfirmDeliveriesRecursiveRunner(XfcdClient xfcdClient) {
        return new GetDataAndConfirmDeliveriesRecursiveRunner(
                xfcdClient,
                trafficsoftApiRestProperties.getContractId()
        );
    }

    @Bean("trafficsoftApiRestClientSetterFactory")
    public SetterFactory setterFactory() {
        return (target, method) -> {
            String groupKey = target.name();
            String commandKey = Feign.configKey(target.type(), method);

            HystrixThreadPoolProperties.Setter threadPoolProperties = HystrixThreadPoolProperties.Setter()
                    .withCoreSize(1);

            HystrixCommandProperties.Setter commandProperties = HystrixCommandProperties.Setter()
                    .withRequestLogEnabled(true)
                    .withFallbackEnabled(false)
                    .withExecutionTimeoutEnabled(true)
                    .withExecutionTimeoutInMilliseconds((int) SECONDS.toMillis(45))
                    .withExecutionIsolationStrategy(SEMAPHORE)
                    .withExecutionIsolationSemaphoreMaxConcurrentRequests(20);

            return HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                    .andThreadPoolPropertiesDefaults(threadPoolProperties)
                    .andCommandPropertiesDefaults(commandProperties);
        };
    }

}
