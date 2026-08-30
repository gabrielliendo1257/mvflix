package com.gcorp.service.app.mvflix_media_ingestion.application;
import reactor.core.publisher.Mono; import java.util.UUID;
public interface InboxRepository { Mono<Boolean> receive(UUID eventId,String type); Mono<Boolean> completed(UUID eventId); Mono<Void> markCompleted(UUID eventId); Mono<Void> markFailed(UUID eventId,String error); }
