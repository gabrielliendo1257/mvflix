package com.guille.media.reproductor.uploader.advisors;

import com.guille.media.reproductor.uploader.advisors.models.ErrorResponse;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.BucketNotFoundException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.ExceededQuotaException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.IllegalStateTransitionException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.InvalidObjectContentError;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryAccessDeniedException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryPathInvalidException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryPathNotAllowedException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.LibraryRootUnavailableException;
import com.guille.media.reproductor.uploader.storage.library.domain.exception.ScanLimitExceededException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.ObjectAlreadyExistsException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectNotAvailable;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.UserStorageNotFoundException;
import com.guille.media.reproductor.uploader.storage.shared.error.EntityNotFound;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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

    @ExceptionHandler(IllegalStateTransitionException.class)
    public ResponseEntity<?> illegalStateTransition(IllegalStateTransitionException ex) {
        return error(HttpStatus.CONFLICT, "ILLEGAL_STATE_TRANSITION", ex);
    }

    @ExceptionHandler(InvalidObjectContentError.class)
    public ResponseEntity<?> invalidObjectContent(InvalidObjectContentError ex) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_OBJECT_CONTENT", ex);
    }

    @ExceptionHandler(StorageObjectNotAvailable.class)
    public ResponseEntity<?> storageObjectNotAvailable(StorageObjectNotAvailable ex) {
        return error(HttpStatus.NOT_FOUND, "STORAGE_OBJECT_NOT_AVAILABLE", ex);
    }

    @ExceptionHandler(
        com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectMismatchException.class)
    public ResponseEntity<?> storageObjectMismatch(
        com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageObjectMismatchException ex) {
        return error(HttpStatus.CONFLICT, "OBJECT_MISMATCH", ex);
    }

    @ExceptionHandler(EntityNotFound.class)
    public ResponseEntity<?> entityNotFound(EntityNotFound ex) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex);
    }

    @ExceptionHandler(LibraryPathInvalidException.class)
    public ResponseEntity<?> libraryPathInvalid(LibraryPathInvalidException ex) {
        return error(HttpStatus.BAD_REQUEST, "LIBRARY_PATH_INVALID", ex);
    }

    @ExceptionHandler(LibraryPathNotAllowedException.class)
    public ResponseEntity<?> libraryPathNotAllowed(LibraryPathNotAllowedException ex) {
        return error(HttpStatus.BAD_REQUEST, "LIBRARY_PATH_NOT_ALLOWED", ex);
    }

    @ExceptionHandler(LibraryRootUnavailableException.class)
    public ResponseEntity<?> libraryRootUnavailable(LibraryRootUnavailableException ex) {
        return error(HttpStatus.CONFLICT, "LIBRARY_ROOT_UNAVAILABLE", ex);
    }

    @ExceptionHandler(ScanLimitExceededException.class)
    public ResponseEntity<?> scanLimitExceeded(ScanLimitExceededException ex) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "SCAN_TOO_LARGE", ex);
    }

    @ExceptionHandler(LibraryAccessDeniedException.class)
    public ResponseEntity<?> libraryAccessDenied(LibraryAccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "LIBRARY_ACCESS_DENIED", ex);
    }

    @ExceptionHandler(LibraryAlreadyExistsException.class)
    public ResponseEntity<?> libraryAlreadyExists(LibraryAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, "LIBRARY_ALREADY_EXISTS", ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> responseStatus(ResponseStatusException ex) {
        return error(HttpStatus.valueOf(ex.getStatusCode().value()), "HTTP_STATUS", ex);
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
