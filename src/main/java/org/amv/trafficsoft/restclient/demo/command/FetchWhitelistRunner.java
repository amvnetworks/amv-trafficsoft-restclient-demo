package org.amv.trafficsoft.restclient.demo.command;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.amv.trafficsoft.rest.carsharing.whitelist.model.FetchWhitelistsResponseRestDto;
import org.amv.trafficsoft.rest.client.carsharing.whitelist.CarSharingWhitelistClient;
import org.amv.trafficsoft.rest.xfcd.model.NodeRestDto;
import org.springframework.boot.CommandLineRunner;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
public class FetchWhitelistRunner implements CommandLineRunner {

    private final CarSharingWhitelistClient carSharingWhitelistClient;
    private final long contractId;
    private final List<Long> vehicleIds;

    public FetchWhitelistRunner(CarSharingWhitelistClient carSharingWhitelistClient, long contractId, List<Long> vehicleIds) {
        this.carSharingWhitelistClient = requireNonNull(carSharingWhitelistClient);
        this.contractId = contractId;
        this.vehicleIds = ImmutableList.copyOf(vehicleIds);
    }

    @Override
    public void run(String... args) throws Exception {
        Action1<FetchWhitelistsResponseRestDto> onNext = whitelistsResponseRestDto -> {
            log.info("Received Whitelist Response with {} whitelists", whitelistsResponseRestDto.getVehicleWhitelists().size());
            whitelistsResponseRestDto.getVehicleWhitelists().forEach(wl -> log.info("Received Whitelist: {}", wl));
        };
        Action1<Throwable> onError = error -> {
            log.error("{}", error);
        };
        Action0 onComplete = () -> {
            log.info("Completed.");
        };

        Scheduler sameThreadExecutor = Schedulers.immediate();

        log.info("==================================================");
        this.carSharingWhitelistClient.fetchWhitelists(this.contractId, this.vehicleIds)
                .toObservable()
                .observeOn(sameThreadExecutor)
                .subscribeOn(sameThreadExecutor)
                .subscribe(onNext, onError, onComplete);
        log.info("==================================================");
    }
}
