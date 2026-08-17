package com.gcorp.service.app.mvflix_movies.advisors;

import com.gcorp.service.app.mvflix_movies.advisors.models.ErrorResponse;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.domain.movie.MovieNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> unauthenticated(
            AuthenticationCredentialsNotFoundException ex) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex);
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ErrorResponse> movieNotFound(MovieNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "MOVIE_NOT_FOUND", ex);
    }

    @ExceptionHandler(MovieConflictException.class)
    public ResponseEntity<ErrorResponse> movieConflict(MovieConflictException ex) {
        return error(HttpStatus.CONFLICT, "MOVIE_CONFLICT", ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> globalError(Exception ex) {
        log.error(ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex);
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, Exception ex) {
        log.warn("{}: {}", code, ex.getMessage());
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), code, ex.getMessage()));
    }
}
