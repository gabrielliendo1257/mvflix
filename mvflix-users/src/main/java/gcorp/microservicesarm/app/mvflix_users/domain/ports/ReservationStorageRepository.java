package gcorp.microservicesarm.app.mvflix_users.domain.ports;

import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageReservations;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import reactor.core.publisher.Mono;

public interface ReservationStorageRepository {
    Mono<StorageReservations> findByUserId(UserId userId);
}
