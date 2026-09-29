package ru.leti.wise.task.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.grpc.client.ImportGrpcClients;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
@ImportGrpcClients()
public class WiseTaskGatewayApplication {

    static void main(String[] args) {
        log.info("wise-task-gateway application, starting");
        SpringApplication.run(WiseTaskGatewayApplication.class, args);
    }

}
