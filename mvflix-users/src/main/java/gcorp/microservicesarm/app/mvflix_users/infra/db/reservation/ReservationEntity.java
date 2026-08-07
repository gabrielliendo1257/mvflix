package gcorp.microservicesarm.app.mvflix_users.infra.db.reservation;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import gcorp.microservicesarm.app.mvflix_users.domain.models.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@Table(name = "user_storage_reservations")
@ToString
@AllArgsConstructor
public class ReservationEntity {
    @Id
    private Long id;
    private UUID userId;
    private Long bytes;
    private ReservationStatus status;
    private Instant createdAt;
    private Instant expiresAt;
}
