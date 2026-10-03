package ru.leti.wise.task.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GraphQlMetrics {

    private static final String METRIC_NAME = "graphql.requests";
    private static final String OPERATION_TAG = "operation";

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public void increment(String operation) {
        counters.computeIfAbsent(operation, name -> Counter.builder(METRIC_NAME)
                        .description("GraphQL request count per operation")
                        .tag(OPERATION_TAG, name)
                        .register(meterRegistry))
                .increment();
    }
}
