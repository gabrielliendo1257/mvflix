package com.gcorp.service.app.mvflix_movies.infrastructure.web.error;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieNotFoundException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetAlreadyIdentifiedException;
import com.gcorp.service.app.mvflix_movies.library.domain.MediaAssetNotFoundException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

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

    @ExceptionHandler(MediaAssetNotFoundException.class)
    public ResponseEntity<ErrorResponse> mediaAssetNotFound(MediaAssetNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "MEDIA_ASSET_NOT_FOUND", ex);
    }

    @ExceptionHandler(MovieAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> movieAccessDenied(MovieAccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "MOVIE_ACCESS_DENIED", ex);
    }

    @ExceptionHandler(MovieConflictException.class)
    public ResponseEntity<ErrorResponse> movieConflict(MovieConflictException ex) {
        return error(HttpStatus.CONFLICT, "MOVIE_CONFLICT", ex);
    }

    @ExceptionHandler(MediaAssetAlreadyIdentifiedException.class)
    public ResponseEntity<ErrorResponse> mediaAssetConflict(
            MediaAssetAlreadyIdentifiedException ex) {
        return error(HttpStatus.CONFLICT, "MEDIA_ASSET_ALREADY_IDENTIFIED", ex);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ErrorResponse> validation(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .findFirst()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> globalError(Exception ex) {
        log.error("Unhandled error", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Unexpected server error");
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, Exception ex) {
        return error(status, code, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
        log.warn("{}: {}", code, message);
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), code, message));
    }
}
