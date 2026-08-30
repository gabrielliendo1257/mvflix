package com.gcorp.service.app.mvflix_media_ingestion.application;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
public interface CompensationRepository { Mono<Void> schedule(UUID ingestionId,String action); Flux<Compensation> due(int limit); Mono<Void> success(UUID id); Mono<Void> failure(UUID id,String error); record Compensation(UUID id,UUID ingestionId,String action) {} }
