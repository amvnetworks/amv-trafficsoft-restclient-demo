package org.amv.trafficsoft.restclient.demo.command;

import lombok.extern.slf4j.Slf4j;
import org.amv.trafficsoft.rest.client.xfcd.XfcdClient;
import org.amv.trafficsoft.rest.xfcd.model.DeliveryRestDto;
import org.amv.trafficsoft.rest.xfcd.model.NodeRestDto;
import org.amv.trafficsoft.rest.xfcd.model.TrackRestDto;
import org.springframework.boot.CommandLineRunner;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@Slf4j
public class GetDataAndConfirmDeliveriesRecursiveRunner implements CommandLineRunner {

    private final XfcdClient xfcdClient;
    private final long contractId;

    public GetDataAndConfirmDeliveriesRecursiveRunner(XfcdClient xfcdClient, long contractId) {
        this.xfcdClient = requireNonNull(xfcdClient);
        this.contractId = contractId;
    }

    @Override
    public void run(String... args) throws Exception {
        int amountOfRequests = 100;
        long delay = 1;
        TimeUnit delayTimeUnit = TimeUnit.SECONDS;

        CountDownLatch latch = new CountDownLatch(amountOfRequests);

        Action1<Throwable> onError = error -> {
            log.error("{}", error);

            LongStream.range(0, latch.getCount())
                    .forEach(foo -> latch.countDown());
        };
        Action0 onComplete = () -> {
            log.info("Completed.");
            latch.countDown();
        };

        Action1<List<DeliveryRestDto>> onNext = new Action1<List<DeliveryRestDto>>() {
            @Override
            public void call(List<DeliveryRestDto> deliveryRestDtos) {
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

                NodeRestDto firstNodeOrNull = deliveryRestDtos.stream()
                        .flatMap(deliveryRestDto -> deliveryRestDto.getTrack().stream())
                        .flatMap(track -> track.getNodes().stream())
                        .findFirst()
                        .orElse(null);

                log.info("Got deliveries: {} ", deliveryIds);
                log.info("Amount of tracks: {} ", amountOfTracks);
                log.info("Amount of nodes: {} ", amountOfNodes);
                log.info("First Node: {}", firstNodeOrNull);

                getDataAndConfirmDeliveries(deliveryIds)
                        .delay(delay, delayTimeUnit)
                        .subscribe(this, onError, onComplete);
            }
        };


        log.info("==================================================");
        List<Long> deliveryIds = Collections.emptyList();
        getDataAndConfirmDeliveries(deliveryIds)
                .delay(delay, delayTimeUnit)
                .subscribe(onNext, onError, onComplete);

        latch.await();
        log.info("==================================================");
    }

    private Observable<List<DeliveryRestDto>> getDataAndConfirmDeliveries(List<Long> deliveryIds) {
        Scheduler sameThreadExecutor = Schedulers.immediate();

        return this.xfcdClient.getDataAndConfirmDeliveries(this.contractId, deliveryIds)
                .toObservable()
                .observeOn(sameThreadExecutor)
                .subscribeOn(sameThreadExecutor);
    }
}
