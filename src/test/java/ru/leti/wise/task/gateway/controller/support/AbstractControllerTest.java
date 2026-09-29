package ru.leti.wise.task.gateway.controller.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import ru.leti.wise.task.gateway.configuration.JwtProperties;
import ru.leti.wise.task.gateway.controller.AuthController;
import ru.leti.wise.task.gateway.controller.GraphController;
import ru.leti.wise.task.gateway.controller.PluginController;
import ru.leti.wise.task.gateway.controller.ProfileController;
import ru.leti.wise.task.gateway.controller.StatisticsController;
import ru.leti.wise.task.gateway.controller.TaskController;
import ru.leti.wise.task.gateway.mapper.GraphMapperImpl;
import ru.leti.wise.task.gateway.mapper.PaginationMapperImpl;
import ru.leti.wise.task.gateway.mapper.PluginMapperImpl;
import ru.leti.wise.task.gateway.mapper.ProfileMapperImpl;
import ru.leti.wise.task.gateway.mapper.SolutionMapperImpl;
import ru.leti.wise.task.gateway.mapper.StatisticMapperImpl;
import ru.leti.wise.task.gateway.mapper.TaskMapperImpl;
import ru.leti.wise.task.gateway.service.GraphService;
import ru.leti.wise.task.gateway.service.PluginService;
import ru.leti.wise.task.gateway.service.SecurityService;
import ru.leti.wise.task.gateway.service.TaskService;
import ru.leti.wise.task.gateway.service.grpc.graph.GraphGrpcService;
import ru.leti.wise.task.gateway.service.grpc.plugin.PluginGrpcService;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.gateway.service.grpc.statistic.StatisticsGrpcService;
import ru.leti.wise.task.gateway.service.grpc.task.TaskGrpcService;

import java.time.Duration;

/**
 * Поднимает мини-Spring-контекст: реальные мапперы, реальные сервисы и реальные контроллеры,
 * а gRPC-обёртки подменены фейками. Так тесты проверяют полный маппинг и отсутствие NPE.
 */
public abstract class AbstractControllerTest {

    protected static final String CURRENT_USER_ID = "current-user";

    protected FakeTaskGrpcService taskGrpcService;
    protected FakeProfileGrpcService profileGrpcService;
    protected FakePluginGrpcService pluginGrpcService;
    protected FakeGraphGrpcService graphGrpcService;
    protected FakeStatisticsGrpcService statisticsGrpcService;

    protected AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUpGatewayContext() {
        taskGrpcService = new FakeTaskGrpcService();
        profileGrpcService = new FakeProfileGrpcService();
        pluginGrpcService = new FakePluginGrpcService();
        graphGrpcService = new FakeGraphGrpcService();
        statisticsGrpcService = new FakeStatisticsGrpcService();

        JwtEncoder jwtEncoder = parameters -> Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(CURRENT_USER_ID)
                .build();

        context = new AnnotationConfigApplicationContext();
        context.register(
                TaskMapperImpl.class, SolutionMapperImpl.class, ProfileMapperImpl.class,
                PluginMapperImpl.class, GraphMapperImpl.class, PaginationMapperImpl.class, StatisticMapperImpl.class
        );
        context.register(TaskService.class, GraphService.class, PluginService.class, SecurityService.class);
        context.register(
                TaskController.class, GraphController.class, PluginController.class,
                ProfileController.class, StatisticsController.class, AuthController.class
        );
        context.registerBean(TaskGrpcService.class, () -> taskGrpcService);
        context.registerBean(ProfileGrpcService.class, () -> profileGrpcService);
        context.registerBean(PluginGrpcService.class, () -> pluginGrpcService);
        context.registerBean(GraphGrpcService.class, () -> graphGrpcService);
        context.registerBean(StatisticsGrpcService.class, () -> statisticsGrpcService);
        context.registerBean(JwtEncoder.class, () -> jwtEncoder);
        context.registerBean(JwtProperties.class,
                () -> new JwtProperties(null, null, Duration.ofHours(1), Duration.ofDays(7)));
        context.refresh();

        authenticateAs(CURRENT_USER_ID);
    }

    @AfterEach
    void tearDownGatewayContext() {
        SecurityContextHolder.clearContext();
        if (context != null) {
            context.close();
        }
    }

    protected <T> T controller(Class<T> controllerType) {
        return context.getBean(controllerType);
    }

    protected void authenticateAs(String userId) {
        var jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId)
                .claim("role", "AUTHOR")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
