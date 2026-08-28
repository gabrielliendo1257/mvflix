package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.web;

import com.guille.media.reproductor.uploader.storage.managedstorage.application.StoredObjectDeletedOutbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/** Restricted operational action for recovering the Storage confirmation outbox. */
@RestController
@RequestMapping("/admin/outbox")
@ConditionalOnProperty(name = "mvflix.messaging.kafka.enabled", havingValue = "true")
public class StorageOutboxAdministrationController {

    private final StoredObjectDeletedOutbox outbox;
    private final int maxAttempts;

    public StorageOutboxAdministrationController(
            StoredObjectDeletedOutbox outbox,
            @Value("${storage.outbox.max-attempts:10}") int maxAttempts) {
        this.outbox = outbox;
        this.maxAttempts = maxAttempts;
    }

    @PostMapping("/stored-object-deleted/reactivate")
    public Mono<ResponseEntity<Void>> reactivate() {
        return this.outbox.reactivateExhausted(this.maxAttempts)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
