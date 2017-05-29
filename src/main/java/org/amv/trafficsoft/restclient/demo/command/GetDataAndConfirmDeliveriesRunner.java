package org.amv.trafficsoft.restclient.demo.command;

import lombok.extern.slf4j.Slf4j;
import org.amv.trafficsoft.rest.client.xfcd.XfcdClient;
import org.amv.trafficsoft.rest.xfcd.model.DeliveryRestDto;
import org.amv.trafficsoft.rest.xfcd.model.TrackRestDto;
import org.springframework.boot.CommandLineRunner;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Slf4j
public class GetDataAndConfirmDeliveriesRunner implements CommandLineRunner {

    private final XfcdClient xfcdClient;
    private final long contractId;

    public GetDataAndConfirmDeliveriesRunner(XfcdClient xfcdClient, long contractId) {
        this.xfcdClient = requireNonNull(xfcdClient);
        this.contractId = contractId;
    }

    @Override
    public void run(String... args) throws Exception {
        long delay = 2;
        TimeUnit delayTimeUnit = TimeUnit.SECONDS;

        CountDownLatch latch = new CountDownLatch(1);

        Action1<List<DeliveryRestDto>> onNext = deliveryRestDtos -> {
            List<Long> deliveryIds = deliveryRestDtos.stream()
                    .map(DeliveryRestDto::getDeliveryId)
                    .collect(toList());

            long amountOfTracks = deliveryRestDtos.stream()
                    .map(DeliveryRestDto::getTrack)
                    .mapToLong(Collection::size)
                    .sum();

            long amountOfNodes = deliveryRestDtos.stream()
                    .map(DeliveryRestDto::getTrack)
                    .flatMap(Collection::stream)
                    .map(TrackRestDto::getNodes)
                    .mapToLong(Collection::size)
                    .sum();

            log.info("Got deliveries: {} ", deliveryIds);
            log.info("Amount of tracks: {} ", amountOfTracks);
            log.info("Amount of nodes: {} ", amountOfNodes);
        };
        Action1<Throwable> onError = error -> {
            log.error("{}", error);

            LongStream.range(0, latch.getCount())
                    .forEach(foo -> latch.countDown());
        };
        Action0 onComplete = () -> {
            log.info("Completed.");
            latch.countDown();
        };

        Scheduler sameThreadExecutor = Schedulers.immediate();

        log.info("==================================================");
        this.xfcdClient.getDataAndConfirmDeliveries(contractId, Collections.emptyList())
                .toObservable()
                .observeOn(sameThreadExecutor)
                .subscribeOn(sameThreadExecutor)
                .map(deliveryRestDtos -> deliveryRestDtos.stream()
                        .map(DeliveryRestDto::getDeliveryId)
                        .collect(toList()))
                .flatMap(deliveryIds -> this.xfcdClient.getDataAndConfirmDeliveries(contractId, deliveryIds)
                        .toObservable())
                .doOnNext(onNext)
                .map(deliveryRestDtos -> deliveryRestDtos.stream()
                        .map(DeliveryRestDto::getDeliveryId)
                        .collect(toList()))
                .delay(delay, delayTimeUnit)
                .flatMap(deliveryIds -> this.xfcdClient.getDataAndConfirmDeliveries(contractId, deliveryIds)
                        .toObservable())
                .doOnNext(onNext)
                .map(deliveryRestDtos -> deliveryRestDtos.stream()
                        .map(DeliveryRestDto::getDeliveryId)
                        .collect(toList()))
                .delay(delay, delayTimeUnit)
                .flatMap(deliveryIds -> this.xfcdClient.getDataAndConfirmDeliveries(contractId, deliveryIds)
                        .toObservable())
                .doOnNext(onNext)
                .map(deliveryRestDtos -> deliveryRestDtos.stream()
                        .map(DeliveryRestDto::getDeliveryId)
                        .collect(toList()))
                .delay(delay, delayTimeUnit)
                .flatMap(deliveryIds -> this.xfcdClient.getDataAndConfirmDeliveries(contractId, deliveryIds)
                        .toObservable())
                .delay(delay, delayTimeUnit)
                .subscribe(onNext::call, onError, onComplete);

        latch.await();
        log.info("==================================================");
    }
}
