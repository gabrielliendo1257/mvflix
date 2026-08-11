package com.guille.media.reproductor.uploader.advisors;

import com.guille.media.reproductor.uploader.advisors.models.ErrorResponse;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.ObjectAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.domain.exceptions.UserStorageNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<?> unauthenticated(AuthenticationCredentialsNotFoundException ex) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", ex);
    }

    @ExceptionHandler(UserStorageNotFoundException.class)
    public ResponseEntity<?> userStorageNotFound(UserStorageNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "USER_STORAGE_NOT_FOUND", ex);
    }

    @ExceptionHandler(BucketNotFoundException.class)
    public ResponseEntity<?> bucketNotFound(BucketNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "BUCKET_NOT_FOUND", ex);
    }

    @ExceptionHandler(ExceededQuotaException.class)
    public ResponseEntity<?> quotaExceeded(ExceededQuotaException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "QUOTA_EXCEEDED", ex);
    }

    @ExceptionHandler(ObjectAlreadyExistsException.class)
    public ResponseEntity<?> objectAlreadyExists(ObjectAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, "OBJECT_ALREADY_EXISTS", ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalError(Exception ex) {
        log.error(ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", ex);
    }

    private ResponseEntity<?> error(HttpStatus status, String code, Exception ex) {
        log.warn("{}: {}", code, ex.getMessage());
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(status.value(), code, ex.getMessage(), LocalDateTime.now()));
    }
}
