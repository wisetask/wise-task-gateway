package ru.leti.wise.task.gateway.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;
import ru.leti.wise.task.graph.GraphServiceGrpc;
import ru.leti.wise.task.plugin.PluginServiceGrpc;
import ru.leti.wise.task.profile.ProfileServiceGrpc;
import ru.leti.wise.task.task.TaskServiceGrpc;
import ru.leti.wise.task.event.StatisticsServiceGrpc;

@Configuration
@ImportGrpcClients(
        target = "${grpc.service.graph.host}:${grpc.service.graph.port}",
        types = GraphServiceGrpc.GraphServiceBlockingStub.class
)
@ImportGrpcClients(
        target = "${grpc.service.plugin.host}:${grpc.service.plugin.port}",
        types = PluginServiceGrpc.PluginServiceBlockingStub.class
)
@ImportGrpcClients(
        target = "${grpc.service.profile.host}:${grpc.service.profile.port}",
        types = ProfileServiceGrpc.ProfileServiceBlockingStub.class
)
@ImportGrpcClients(
        target = "${grpc.service.task.host}:${grpc.service.task.port}",
        types = TaskServiceGrpc.TaskServiceBlockingStub.class
)
@ImportGrpcClients(
        target = "${grpc.service.statistics.host}:${grpc.service.statistics.port}",
        types = StatisticsServiceGrpc.StatisticsServiceBlockingStub.class
)
public class GrpcConfiguration {
}