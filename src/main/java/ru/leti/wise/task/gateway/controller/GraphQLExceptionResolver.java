package ru.leti.wise.task.gateway.controller;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.exception.InvalidRefreshTokenException;

@Slf4j
@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (isUnauthorized(ex)) {
            log.warn("graphql request, unauthenticated at path {}: {}",
                    env.getExecutionStepInfo().getPath(), ex.getMessage());
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message(ex.getMessage())
                    .path(env.getExecutionStepInfo().getPath())
                    .location(env.getField().getSourceLocation())
                    .build();
        }
        log.error("graphql request, failed at path {}", env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.INTERNAL_ERROR)
                .message(ex.getMessage())
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .build();
    }

    private boolean isUnauthorized(Throwable ex) {
        if (ex instanceof InvalidRefreshTokenException) {
            return true;
        }
        return ex instanceof StatusRuntimeException e && e.getStatus() == Status.UNAUTHENTICATED;
    }
}
