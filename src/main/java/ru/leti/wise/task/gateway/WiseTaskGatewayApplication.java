package ru.leti.wise.task.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.grpc.client.ImportGrpcClients;

@SpringBootApplication
@ConfigurationPropertiesScan
@ImportGrpcClients()
public class WiseTaskGatewayApplication {

    static void main(String[] args) {
        SpringApplication.run(WiseTaskGatewayApplication.class, args);
    }

}
