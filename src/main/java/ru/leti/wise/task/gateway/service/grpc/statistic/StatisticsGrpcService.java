package ru.leti.wise.task.gateway.service.grpc.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.event.Statistic;
import ru.leti.wise.task.event.StatisticsServiceGrpc.StatisticsServiceBlockingStub;


@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsGrpcService {
    private final StatisticsServiceBlockingStub statisticsService;

    public Statistic.StatisticResponse getStatistic(Statistic.StatisticRequest request){
        log.debug("StatisticsService.getStatistic request, {}", request);
        var response = statisticsService.getStatistic(request);
        log.debug("StatisticsService.getStatistic request, fetched statistic");
        return response;
    }
}
