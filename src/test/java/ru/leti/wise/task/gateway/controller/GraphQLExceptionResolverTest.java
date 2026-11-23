package ru.leti.wise.task.gateway.controller;

import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.AccessDeniedException;

import io.grpc.Status;
import org.springframework.graphql.execution.ErrorType;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import io.grpc.StatusRuntimeException;
import ru.leti.wise.task.gateway.exception.InvalidRefreshTokenException;


@ExtendWith(MockitoExtension.class)
public class GraphQLExceptionResolverTest {
    private GraphQLExceptionResolver resolver;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataFetchingEnvironment env;

    @BeforeEach 
    void setUp(){
        resolver = new GraphQLExceptionResolver();
    }

    @Test
    void notFoundTest(){
        StatusRuntimeException ex = Status.NOT_FOUND
            .withDescription("Task not found")
            .asRuntimeException();
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.NOT_FOUND, result.getErrorType());
        assertEquals("Task not found", result.getMessage());
    }

    @Test
    void notFoundTestWitohtDiscription(){
        StatusRuntimeException ex = Status.NOT_FOUND
            .withDescription(null)
            .asRuntimeException();
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.NOT_FOUND, result.getErrorType());
        assertEquals("NOT_FOUND", ex.getMessage());
    }

    @Test
    void unknowTest(){
        StatusRuntimeException ex = Status.UNKNOWN
            .withDescription("Unknown task")
            .asRuntimeException();
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.INTERNAL_ERROR, result.getErrorType());
        assertEquals("Unknown task", result.getMessage());
    }

    @Test 
    void invalidArgumentTest(){
        StatusRuntimeException ex = Status.INVALID_ARGUMENT
            .withDescription("Invalid argument")
            .asRuntimeException();
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
        assertEquals("Invalid argument", result.getMessage());
    }

    @Test 
    void unauthenticatedTest(){
        StatusRuntimeException ex = Status.UNAUTHENTICATED
            .withDescription("unauthenticated")
            .asRuntimeException();
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.UNAUTHORIZED, result.getErrorType());
        assertEquals("unauthenticated", result.getMessage());
    }

    @Test 
    void accessDeniedTest(){
        AccessDeniedException ex = new AccessDeniedException("FORBIDDEN");
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.FORBIDDEN, result.getErrorType());
        assertEquals("FORBIDDEN", result.getMessage());
    }

    @Test 
    void unknownExceptionTest(){
        RuntimeException ex = new RuntimeException("Something");
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.INTERNAL_ERROR, result.getErrorType());
        assertEquals("Something", result.getMessage());
    }

    @Test 
    void invalidRefreshTokenTest(){
        InvalidRefreshTokenException ex = new InvalidRefreshTokenException("unauthenticated");
        
        GraphQLError result = resolver.resolveToSingleError(ex, env);

        assertNotNull(result);
        assertEquals(ErrorType.UNAUTHORIZED, result.getErrorType());
        assertEquals("unauthenticated", result.getMessage());
    }
}
