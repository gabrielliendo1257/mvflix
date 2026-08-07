package gcorp.microservicesarm.app.mvflix_users.infra.db.users;

import java.util.UUID;

import org.mapstruct.Mapper;

import gcorp.microservicesarm.app.mvflix_users.domain.models.Email;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageQuota;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageUsage;
import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Username;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);

    /* ---------- UserId ---------- */

    default UserId map(UUID id) {
        return id == null ? null : new UserId(id);
    }

    default UUID map(UserId id) {
        return id == null ? null : id.value();
    }

    /* ---------- Username ---------- */

    default Username mapUsername(String username) {
        return username == null ? null : new Username(username);
    }

    default String map(Username username) {
        return username == null ? null : username.value();
    }

    /* ---------- Email ---------- */

    default Email mapEmail(String email) {
        return email == null ? null : new Email(email);
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    /* ---------- StorageQuota ---------- */

    default StorageQuota mapQuota(long quota) {
        return new StorageQuota(quota);
    }

    default long map(StorageQuota quota) {
        return quota.getUserBytesQuota();
    }

    /* ---------- StorageUsage ---------- */

    default StorageUsage mapUsage(long usage) {
        return new StorageUsage(usage);
    }

    default long map(StorageUsage usage) {
        return usage.getUserCurrentBytesUsage();
    }
}
