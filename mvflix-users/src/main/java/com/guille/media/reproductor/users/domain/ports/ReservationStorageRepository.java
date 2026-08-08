package com.guille.media.reproductor.users.domain.ports;

import com.guille.media.reproductor.users.domain.models.StorageReservations;
import com.guille.media.reproductor.users.domain.models.UserId;
import reactor.core.publisher.Mono;

public interface ReservationStorageRepository {
    Mono<StorageReservations> findByUserId(UserId userId);
}
