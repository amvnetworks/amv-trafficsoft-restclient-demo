package org.amv.trafficsoft.restclient.demo.command;

import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.amv.trafficsoft.rest.asgregister.model.ModelRestDto;
import org.amv.trafficsoft.rest.asgregister.model.OemRestDto;
import org.amv.trafficsoft.rest.asgregister.model.SeriesRestDto;
import org.amv.trafficsoft.rest.client.asgregister.AsgRegisterClient;
import org.springframework.boot.CommandLineRunner;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
public class AllSeriesAndModelsOfOemRunner implements CommandLineRunner {

    private final AsgRegisterClient asgRegisterClient;
    private final long contractId;

    public AllSeriesAndModelsOfOemRunner(AsgRegisterClient asgRegisterClient, long contractId) {
        this.asgRegisterClient = requireNonNull(asgRegisterClient);
        this.contractId = contractId;
    }

    @Override
    public void run(String... args) throws Exception {
        Action1<SeriesWithModels> onNext = seriesWithModels -> {
            log.info("Received SeriesWithModels: {}", seriesWithModels);
        };
        Action1<Throwable> onError = error -> {
            log.error("{}", error);
        };
        Action0 onComplete = () -> {
            log.info("Completed.");
        };

        Scheduler sameThreadExecutor = Schedulers.immediate();

        // fetch all series of an oem
        Func1<OemRestDto, Observable<SeriesRestDto>> fetchSeriesOfOem = oem -> asgRegisterClient
                .getSeries(contractId, oem.getOemCode())
                .toObservable()
                .flatMap(seriesResponse -> Observable.from(seriesResponse.getSeries()));


        // fetch all models of a series and transform it to objects holding the series and a list of models
        Func1<SeriesRestDto, Observable<ModelRestDto>> fetchModelsOfSeries = series -> asgRegisterClient
                .getModels(contractId, series.getOemCode(), series.getSeriesCode())
                .toObservable()
                .flatMap(modelsResponse -> Observable.from(modelsResponse.getModels()));

        log.info("==================================================");
        this.asgRegisterClient.getOems(contractId)
                .toObservable()
                .observeOn(sameThreadExecutor)
                .subscribeOn(sameThreadExecutor)
                .flatMap(oemResponse -> Observable.from(oemResponse.getOems()))
                .flatMap(fetchSeriesOfOem)
                .flatMap(series -> fetchModelsOfSeries.call(series)
                        .toList()
                        .map(modelList -> SeriesWithModels.builder()
                                .series(series)
                                .models(modelList)
                                .build()))
                .subscribe(onNext, onError, onComplete);
        log.info("==================================================");
    }

    @Value
    @Builder
    private static class SeriesWithModels {
        private SeriesRestDto series;
        private List<ModelRestDto> models;
    }
}
