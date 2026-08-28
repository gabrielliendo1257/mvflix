package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ManagedDeletionOutbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/** Restricted operational action for recovering an exhausted managed deletion. */
@RestController
@RequestMapping("/admin/outbox")
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class OutboxAdministrationController {

    private final ManagedDeletionOutbox outbox;
    private final int maxAttempts;

    public OutboxAdministrationController(
            ManagedDeletionOutbox outbox,
            @Value("${movies.outbox.max-attempts:10}") int maxAttempts) {
        this.outbox = outbox;
        this.maxAttempts = maxAttempts;
    }

    @PostMapping("/managed-deletions/{movieId}/reactivate")
    public Mono<ResponseEntity<Void>> reactivate(@PathVariable long movieId) {
        if (movieId <= 0) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return this.outbox.reactivateExhausted(Long.toString(movieId), this.maxAttempts)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
