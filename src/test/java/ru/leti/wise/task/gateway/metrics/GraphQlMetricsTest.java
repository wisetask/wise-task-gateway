package ru.leti.wise.task.gateway.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQlMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GraphQlMetrics metrics = new GraphQlMetrics(meterRegistry);

    @Test
    void incrementsCounterForEveryOperationCall() {
        metrics.increment("getTask");
        metrics.increment("getTask");
        metrics.increment("createTaskGraph");

        assertThat(counter("getTask")).isEqualTo(2d);
        assertThat(counter("createTaskGraph")).isEqualTo(1d);
    }

    @Test
    void keepsSeparateCountersPerOperation() {
        metrics.increment("getTask");
        metrics.increment("getPlugin");
        metrics.increment("getPlugin");

        assertThat(counter("getTask")).isEqualTo(1d);
        assertThat(counter("getPlugin")).isEqualTo(2d);
    }

    @Test
    void reusesTheSameMeterForRepeatedCalls() {
        metrics.increment("getTask");
        metrics.increment("getTask");

        assertThat(meterRegistry.getMeters()).hasSize(1);
        assertThat(meterRegistry.get("graphql.requests").tag("operation", "getTask").counter().getId().getDescription())
                .isEqualTo("GraphQL request count per operation");
    }

    @Test
    void isScrapedByPrometheusAsCounterWithOperationTag() {
        var prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        var prometheusMetrics = new GraphQlMetrics(prometheusRegistry);

        prometheusMetrics.increment("getTask");
        prometheusMetrics.increment("getTask");

        assertThat(prometheusRegistry.scrape())
                .containsPattern("graphql_requests_total\\{operation=\"getTask\"\\}\\s+2(\\.0)?");
    }

    private double counter(String operation) {
        return meterRegistry.get("graphql.requests").tag("operation", operation).counter().count();
    }
}
