package org.amv.trafficsoft.restclient.demo;

import com.netflix.hystrix.*;
import feign.Feign;
import feign.hystrix.SetterFactory;
import org.amv.trafficsoft.rest.client.ClientConfig;
import org.amv.trafficsoft.rest.client.ClientConfig.ConfigurableClientConfig;
import org.amv.trafficsoft.rest.client.TrafficsoftClients;
import org.amv.trafficsoft.rest.client.asgregister.AsgRegisterClient;
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
import static com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.SECONDS;

@Configuration
@EnableConfigurationProperties(CustomerProperties.class)
public class RestClientDemoConfig {

    private final CustomerProperties customerProperties;

    @Autowired
    public RestClientDemoConfig(CustomerProperties customerProperties) {
        this.customerProperties = requireNonNull(customerProperties);
    }

    @Bean
    @Profile("get-last-data-demo")
    public CommandLineRunner lastDataRunner() {
        return new LastDataRunner(
                xfcdClient(),
                customerProperties.getContractId(),
                customerProperties.getVehicleIds()
        );
    }

    @Bean
    @Profile("get-oems-demo")
    public CommandLineRunner allSeriesAndModelsOfOemRunner() {
        return new AllSeriesAndModelsOfOemRunner(
                asgRegisterClient(),
                customerProperties.getContractId()
        );
    }

    @Bean
    @Profile("get-data-and-confirm-deliveries-demo")
    public CommandLineRunner getDataAndConfirmDeliveriesRunner() {
        return new GetDataAndConfirmDeliveriesRunner(
                xfcdClient(),
                customerProperties.getContractId()
        );
    }

    @Bean
    @Profile("get-data-and-confirm-deliveries-recursive-demo")
    public CommandLineRunner getDataAndConfirmDeliveriesRecursiveRunner() {
        return new GetDataAndConfirmDeliveriesRecursiveRunner(
                xfcdClient(),
                customerProperties.getContractId()
        );
    }

    @Bean
    public AsgRegisterClient asgRegisterClient() {
        return TrafficsoftClients.asgRegister(asgRegisterClientConfig());
    }

    @Bean
    public XfcdClient xfcdClient() {
        return TrafficsoftClients.xfcd(xfcdClientConfig());
    }

    @Bean
    public ClientConfig.BasicAuth basicAuth() {
        return ClientConfig.BasicAuthImpl.builder()
                .username(customerProperties.getUsername())
                .password(customerProperties.getPassword())
                .build();
    }


    @Bean
    public ConfigurableClientConfig<AsgRegisterClient> asgRegisterClientConfig() {
        return TrafficsoftClients.config(AsgRegisterClient.class, this.customerProperties.getBaseUrl(), basicAuth())
                .setterFactory(setterFactory())
                .build();
    }

    @Bean
    public ConfigurableClientConfig<XfcdClient> xfcdClientConfig() {
        return TrafficsoftClients.config(XfcdClient.class, this.customerProperties.getBaseUrl(), basicAuth())
                .setterFactory(setterFactory())
                .build();
    }

    @Bean
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
