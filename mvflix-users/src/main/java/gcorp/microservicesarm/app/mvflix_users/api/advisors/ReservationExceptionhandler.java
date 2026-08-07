package gcorp.microservicesarm.app.mvflix_users.api.advisors;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import gcorp.microservicesarm.app.mvflix_users.api.dto.ApiError;
import gcorp.microservicesarm.app.mvflix_users.app.errors.ExceededQuotaException;

@RestControllerAdvice
public class ReservationExceptionhandler {

    @ExceptionHandler(ExceededQuotaException.class)
    public ResponseEntity<ApiError> reservationExceptionHandler(ExceededQuotaException ex, ServerHttpRequest request) {
        ApiError apiError = ApiError.builder()
                .status(409)
                .detail(ex.getMessage())
                .instance(request.getURI().toString())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(409).body(apiError);
    }
}
