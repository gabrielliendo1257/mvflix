package com.guille.media.reproductor.users.infra.db.reservation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.guille.media.reproductor.users.domain.models.UserId;
import reactor.core.publisher.Mono;

public interface ReservationR2dbcRepository extends ReactiveCrudRepository<ReservationEntity, Long> {
    Mono<ReservationEntity> findByUserId(UserId userId);
}
