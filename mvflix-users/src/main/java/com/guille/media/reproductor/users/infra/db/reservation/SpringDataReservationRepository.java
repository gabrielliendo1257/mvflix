package com.guille.media.reproductor.users.infra.db.reservation;

import com.guille.media.reproductor.users.domain.models.StorageReservations;
import com.guille.media.reproductor.users.domain.models.UserId;
import com.guille.media.reproductor.users.domain.ports.ReservationStorageRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class SpringDataReservationRepository implements ReservationStorageRepository {
    private final ReservationR2dbcRepository reservationR2dbcRepository;
    private final ReservationMapper reservationMapper;

    @Override
    public Mono<StorageReservations> findByUserId(UserId userId) {
        return this.reservationR2dbcRepository.findByUserId(userId)
                .map(this.reservationMapper::toDomain);
    }

}
