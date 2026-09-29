package ru.leti.wise.task.gateway.configuration;

import graphql.scalars.ExtendedScalars;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

@Slf4j
@Configuration
public class GraphQLConfiguration {
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        log.debug("graphql configuration, registering GraphQLLong scalar");
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.GraphQLLong);

    }
}
