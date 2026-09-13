package ru.leti.wise.task.gateway.service.grpc.statistic;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.event.Statistic;
import ru.leti.wise.task.event.StatisticsServiceGrpc.StatisticsServiceBlockingStub;


@Component
@RequiredArgsConstructor
public class StatisticsGrpcService {
    private final StatisticsServiceBlockingStub statisticsService;

    public Statistic.StatisticResponse getStatistic(Statistic.StatisticRequest request){

        return statisticsService.getStatistic(
                request
        );
    }
}
