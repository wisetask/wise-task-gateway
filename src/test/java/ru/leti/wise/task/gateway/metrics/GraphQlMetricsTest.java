package ru.leti.wise.task.gateway.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import ru.leti.wise.task.gateway.dto.UserCredentials;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphQlMetricsTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final GraphQlMetrics metrics = new GraphQlMetrics(meterRegistry);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
    void tagsCounterWithCurrentUser() {
        authenticateAs("user-1");
        metrics.increment("getTask");
        authenticateAs("user-2");
        metrics.increment("getTask");

        assertThat(counterFor("getTask", "user-1")).isEqualTo(1d);
        assertThat(counterFor("getTask", "user-2")).isEqualTo(1d);
    }

    @Test
    void fallsBackToAnonymousUserWithoutAuthentication() {
        metrics.increment("signIn");

        assertThat(counterFor("signIn", "anonymous")).isEqualTo(1d);
    }

    @Test
    void reusesTheSameMeterForRepeatedCallsByTheSameUser() {
        authenticateAs("user-1");
        metrics.increment("getTask");
        metrics.increment("getTask");

        assertThat(meterRegistry.getMeters()).hasSize(1);
        assertThat(meterRegistry.get("graphql.requests").tag("operation", "getTask").counter().getId().getDescription())
                .isEqualTo("GraphQL request count per operation");
    }

    @Test
    void isScrapedByPrometheusAsCounterWithOperationAndUserTags() {
        var prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        var prometheusMetrics = new GraphQlMetrics(prometheusRegistry);

        authenticateAs("user-42");
        prometheusMetrics.increment("getTask");
        prometheusMetrics.increment("getTask");

        assertThat(prometheusRegistry.scrape())
                .containsPattern("graphql_requests_total\\{.*operation=\"getTask\",user=\"user-42\"\\}\\s+2(\\.0)?");
    }

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UserCredentials(
                userId,
                userId + "@example.com",
                "USER",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private double counter(String operation) {
        return meterRegistry.get("graphql.requests").tag("operation", operation).counter().count();
    }

    private double counterFor(String operation, String user) {
        var counter = meterRegistry.find("graphql.requests")
                .tag("operation", operation)
                .tag("user", user)
                .counter();
        return counter == null ? 0d : counter.count();
    }
}
