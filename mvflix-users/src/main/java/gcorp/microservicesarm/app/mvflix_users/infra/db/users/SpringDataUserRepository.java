package gcorp.microservicesarm.app.mvflix_users.infra.db.users;

import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.SimpleUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringDataUserRepository implements SimpleUserRepository {

    private final DatabaseClient databaseClient;
    private final UserMapper userMapper;

    @Override
    public Mono<User> save(User user) {
        log.info("Saved account: {}", user);
        return this.databaseClient
                .sql(
                        """
                INSERT INTO users (
                    id,
                    username,
                    email,
                    plan,
                    enabled,
                    storage_used,
                    storage_quota
                )
                VALUES (
                    :id,
                    :username,
                    :email,
                    :plan,
                    :enabled,
                    :storageUsed,
                    :storageQuota
                )
                RETURNING *
                """)
                .bind("id", user.getId().value())
                .bind("username", user.getUsername().value())
                .bind("email", user.getEmail().value())
                .bind("plan", user.getPlan().name())
                .bind("enabled", user.isEnabled())
                .bind("storageUsed", user.getStorageUsed().getUserCurrentBytesUsage())
                .bind("storageQuota", user.getStorageQuota().getUserBytesQuota())
                .mapProperties(UserEntity.class)
                .one()
                .map(this.userMapper::toDomain);
    }

    @Override
    public Flux<User> findAll() {
        return this.databaseClient
                .sql(
                        """
                SELECT * from users
                """)
                .mapProperties(UserEntity.class)
                .all()
                .map(this.userMapper::toDomain);
    }

    @Override
    public Mono<User> update(User user) {
        log.info("Updated account: {}", user);
        UserEntity entity = this.userMapper.toEntity(user);

        return this.databaseClient
                .sql(
                        """
                UPDATE users
                SET
                    username = :username,
                    email = :email,
                    plan = :plan,
                    enabled = :enabled,
                    storage_used = :storageUsed,
                    storage_quota = :storageQuota,
                    updated_at = NOW()
                WHERE id = :id
                RETURNING *
                """)
                .bind("id", entity.getId())
                .bind("username", entity.getUsername())
                .bind("email", entity.getEmail())
                .bind("plan", entity.getPlan())
                .bind("enabled", entity.isEnabled())
                .bind("storageUsed", entity.getStorageUsed())
                .bind("storageQuota", entity.getStorageQuota())
                .mapProperties(UserEntity.class)
                .one()
                .map(this.userMapper::toDomain);
        // this.databaseClient.sql("UPDATE users SET ")
        // return this.save(user);
    }

    @Override
    public Mono<User> findById(UserId userId) {
        log.info("Finding by id: {}", userId);
        return this.databaseClient
                .sql(
                        """
                SELECT * FROM users WHERE id = :user_id
                """)
                .bind("user_id", userId.value())
                .mapProperties(UserEntity.class)
                .one()
                .map(this.userMapper::toDomain);
    }

    @Override
    public Mono<User> findByUsername(String username) {
		log.info("Finding by username: {}", username);
        return this.databaseClient
                .sql(
                        """
				SELECT * FROM users WHERE username = :username
				""")
                .bind("username", username)
                .mapProperties(UserEntity.class)
                .one()
                .map(this.userMapper::toDomain);
    }
}
