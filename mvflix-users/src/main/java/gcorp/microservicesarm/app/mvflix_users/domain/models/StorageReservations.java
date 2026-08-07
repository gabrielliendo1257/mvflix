package gcorp.microservicesarm.app.mvflix_users.domain.models;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class StorageReservations {
    private Long id;
    private UserId userId;
    private Long bytes;
    private ReservationStatus status;
    private Instant createdAt;
    private Instant expiresAt;
}
