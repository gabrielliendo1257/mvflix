package com.guille.media.reproductor.users.api.advisors;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.guille.media.reproductor.users.api.dto.ApiError;
import com.guille.media.reproductor.users.app.errors.UserNotFoundException;
import com.guille.media.reproductor.users.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.users.domain.exceptions.UserAlreadyExistsException;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiError> userAlreadyExistExceptionHandler(UserAlreadyExistsException ex,
            ServerHttpRequest request) {
        return buildError(409, ex, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> userNotFoundExceptionHandler(UserNotFoundException ex,
            ServerHttpRequest request) {
        return buildError(404, ex, request);
    }

    @ExceptionHandler(ExceededQuotaException.class)
    public ResponseEntity<ApiError> exceededQuotaExceptionHandler(ExceededQuotaException ex,
            ServerHttpRequest request) {
        return buildError(409, ex, request);
    }

    private ResponseEntity<ApiError> buildError(int status, RuntimeException ex, ServerHttpRequest request) {
        ApiError apiError = ApiError.builder()
                .status(status)
                .detail(ex.getMessage())
                .instance(request.getURI().toString())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(apiError);
    }
}
