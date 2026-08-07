package gcorp.microservicesarm.app.mvflix_users.infra.db.users;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

@Data
@Table("users")
public class UserEntity {

    @Id
    private UUID id;
    private final String username;
    private final String email;
    private final String plan;
    private final boolean enabled;
    private final long storageUsed; // TODO Deleted
    private final long storageQuota; // TODO Deleted
    private final Instant createdAt;
    private final Instant updatedAt;
}