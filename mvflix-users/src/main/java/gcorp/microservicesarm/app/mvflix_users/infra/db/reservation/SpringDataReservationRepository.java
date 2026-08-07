package gcorp.microservicesarm.app.mvflix_users.infra.db.reservation;

import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageReservations;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.ReservationStorageRepository;
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
