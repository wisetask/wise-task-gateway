package ru.leti.wise.task.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.StatisticRequestInput;
import ru.leti.graphql.types.StatisticScope;
import ru.leti.graphql.types.StatisticType;
import ru.leti.wise.task.event.Statistic;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsControllerTest extends AbstractControllerTest {

    private StatisticsController controller() {
        return controller(StatisticsController.class);
    }

    @Test
    void getStatistic_mapsRequestAndResponse() throws Exception {
        var input = new StatisticRequestInput();
        input.setType(StatisticType.SUCCESS_RATE);
        input.setScope(StatisticScope.TASK);
        input.setEvent_type("SOLVE");
        input.setTask_id("task-1");
        input.setUser_id("user-1");

        var response = controller().getStatistic(input);

        assertThat(response.getScope()).isEqualTo(StatisticScope.TASK);
        assertThat(response.getType()).isEqualTo(StatisticType.SUCCESS_RATE);
        assertThat(response.getValue()).isEqualTo(0.75);
        assertThat(response.getEvent_type()).isEqualTo("SOLVE");
        assertThat(response.getTask_id()).isEqualTo("task-1");
        assertThat(response.getUser_id()).isEqualTo("user-1");
        assertThat(response.getUpdated_at()).isNotBlank();

        var grpcRequest = statisticsGrpcService.getLastRequest();
        assertThat(grpcRequest).isNotNull();
        assertThat(grpcRequest.getType()).isEqualTo(Statistic.StatisticType.SUCCESS_RATE);
        assertThat(grpcRequest.getScope()).isEqualTo(Statistic.StatisticScope.TASK);
        assertThat(grpcRequest.getEventType()).isEqualTo("SOLVE");
        assertThat(grpcRequest.getTaskId()).isEqualTo("task-1");
        assertThat(grpcRequest.getUserId()).isEqualTo("user-1");

        assertThat(graphQlRequests("getStatistic")).isEqualTo(1d);
    }
}
