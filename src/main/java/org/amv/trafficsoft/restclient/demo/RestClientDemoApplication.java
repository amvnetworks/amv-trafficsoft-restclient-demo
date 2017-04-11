package org.amv.trafficsoft.restclient.demo;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.amv.trafficsoft.rest.client.xfcd.XfcdClient;
import org.amv.trafficsoft.rest.xfcd.model.NodeRestDto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@SpringBootApplication
public class RestClientDemoApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(RestClientDemoApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    public static class LastDataRunner implements CommandLineRunner {

        private XfcdClient xfcdClient;
        private final long contractId;
        private final List<Long> vehicleIds;

        public LastDataRunner(XfcdClient xfcdClient, long contractId, List<Long> vehicleIds) {
            this.xfcdClient = requireNonNull(xfcdClient);
            this.contractId = contractId;
            this.vehicleIds = ImmutableList.copyOf(vehicleIds);
        }

        @Override
        public void run(String... args) throws Exception {
            Action1<List<NodeRestDto>> onNext = nodeRestDtos -> {
                log.info("Received Nodes: {}", nodeRestDtos);
            };
            Action1<Throwable> onError = error -> {
                log.error("{}", error);
            };
            Action0 onComplete = () -> {
                log.info("Completed.");
            };

            Scheduler sameThreadExecutor = Schedulers.immediate();

            log.info("==================================================");
            this.xfcdClient.getLastData(this.contractId, this.vehicleIds)
                    .toObservable()
                    .observeOn(sameThreadExecutor)
                    .subscribeOn(sameThreadExecutor)
                    .subscribe(onNext, onError, onComplete);
            log.info("==================================================");
        }
    }
}
