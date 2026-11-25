package ru.leti.wise.task.gateway.controller;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;

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
        Status grpcStatus = getGrpcStatus(ex);

        if (grpcStatus != null){
            String message = grpcStatus.getDescription() != null ? grpcStatus.getDescription() : ex.getMessage();
            ErrorType graphqlError = mapGrpcToGraphQL(grpcStatus.getCode());

            if (graphqlError != ErrorType.INTERNAL_ERROR){
                return buildClientWarning(graphqlError, message, env);
            }

            return buildClientError(ex, env, message);
        } 

        if (ex instanceof InvalidRefreshTokenException || ex instanceof JwtIssuerException){
            return buildClientWarning(ErrorType.UNAUTHORIZED, ex.getMessage(), env);
        } 

        if (ex instanceof AccessDeniedException){
            return buildClientWarning(ErrorType.FORBIDDEN, ex.getMessage(), env);
        }

        return buildClientError(ex, env, ex.getMessage());
    }

    private Status getGrpcStatus(Throwable ex){
        if (ex instanceof StatusRuntimeException e){
            return e.getStatus();
        }

        if (ex instanceof StatusException e){
            return e.getStatus();
        }

        return null;
    }

    private GraphQLError buildClientWarning(ErrorType errorType, String message, DataFetchingEnvironment env){
        log.warn("graphql request, {} at path {}: {}",
                errorType, env.getExecutionStepInfo().getPath(), message);
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
            case INVALID_ARGUMENT, ALREADY_EXISTS, ABORTED, FAILED_PRECONDITION, OUT_OF_RANGE -> ErrorType.BAD_REQUEST;
            case UNAUTHENTICATED -> ErrorType.UNAUTHORIZED;
            case PERMISSION_DENIED -> ErrorType.FORBIDDEN;
            default -> ErrorType.INTERNAL_ERROR;
        };
    }
}
