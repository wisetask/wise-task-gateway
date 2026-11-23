package ru.leti.wise.task.gateway.controller;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.AccessDeniedException;

import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.gateway.exception.InvalidRefreshTokenException;
import ru.leti.wise.task.gateway.exception.JwtIssuerException;

@Slf4j
@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof StatusRuntimeException grpcEx){
            String message = grpcEx.getStatus().getDescription() != null ? grpcEx.getStatus().getDescription() : grpcEx.getMessage();
            ErrorType graphqlError = mapGrpcToGraphQL(grpcEx.getStatus().getCode());

            if (graphqlError != ErrorType.INTERNAL_ERROR){
                return buildClientWarning(graphqlError, message, grpcEx, env);
            }

            return buildClientError(grpcEx, env, message);
        } 

        if (ex instanceof InvalidRefreshTokenException || ex instanceof JwtIssuerException){
            return buildClientWarning(ErrorType.UNAUTHORIZED, ex.getMessage(), ex, env);
        } 

        if (ex instanceof AccessDeniedException){
            return buildClientWarning(ErrorType.FORBIDDEN, ex.getMessage(), ex, env);
        }

        return buildClientError(ex, env, ex.getMessage());
    }

    private GraphQLError buildClientWarning(ErrorType errorType, String message, Throwable ex, DataFetchingEnvironment env){
        log.warn("graphql request, {} at path {}: {}",
                errorType, env.getExecutionStepInfo().getPath(), ex.getMessage());
        return GraphqlErrorBuilder.newError(env)
                    .errorType(errorType)
                    .message(message)
                    .build(); 
    }

    private GraphQLError buildClientError(Throwable ex, DataFetchingEnvironment env, String message){
        log.error("graphql request, failed at path {}", env.getExecutionStepInfo().getPath(), ex);
        return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message(message)
                    .build(); 
    }


    private ErrorType mapGrpcToGraphQL(Status.Code code){
        return switch (code){
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            case INVALID_ARGUMENT -> ErrorType.BAD_REQUEST;
            case UNAUTHENTICATED -> ErrorType.UNAUTHORIZED;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}
