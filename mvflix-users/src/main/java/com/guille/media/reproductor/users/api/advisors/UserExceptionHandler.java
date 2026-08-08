package com.guille.media.reproductor.users.api.advisors;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.guille.media.reproductor.users.api.dto.ApiError;
import com.guille.media.reproductor.users.domain.exceptions.UserAlreadyExistException;

@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ApiError> userAlreadyExistExceptionHandler(UserAlreadyExistException ex,
            ServerHttpRequest request) {
        ApiError apiError = ApiError.builder()
                .status(400)
                .detail(ex.getMessage())
                .instance(request.getURI().toString())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(apiError);
    }
}
