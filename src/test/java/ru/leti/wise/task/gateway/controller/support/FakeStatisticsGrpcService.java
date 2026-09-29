package ru.leti.wise.task.gateway.controller.support;

import ru.leti.wise.task.event.Statistic.StatisticRequest;
import ru.leti.wise.task.event.Statistic.StatisticResponse;
import ru.leti.wise.task.gateway.service.grpc.statistic.StatisticsGrpcService;

/**
 * Фейковый statistics-сервис: отдаёт заранее подготовленный ответ.
 */
public class FakeStatisticsGrpcService extends StatisticsGrpcService {

    private StatisticResponse response = TestData.statisticResponse();
    private StatisticRequest lastRequest;

    public FakeStatisticsGrpcService() {
        super(null);
    }

    public void setResponse(StatisticResponse response) {
        this.response = response;
    }

    public StatisticRequest getLastRequest() {
        return lastRequest;
    }

    @Override
    public StatisticResponse getStatistic(StatisticRequest request) {
        this.lastRequest = request;
        return response;
    }
}
