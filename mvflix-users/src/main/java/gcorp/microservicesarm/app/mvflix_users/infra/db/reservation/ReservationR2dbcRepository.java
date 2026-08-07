package gcorp.microservicesarm.app.mvflix_users.infra.db.reservation;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import reactor.core.publisher.Mono;

public interface ReservationR2dbcRepository extends ReactiveCrudRepository<ReservationEntity, Long> {
    Mono<ReservationEntity> findByUserId(UserId userId);
}
