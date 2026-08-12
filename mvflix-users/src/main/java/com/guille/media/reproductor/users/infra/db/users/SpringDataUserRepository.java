package com.guille.media.reproductor.users.infra.db.users;

import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.UserId;
import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;

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
                    enabled
                )
                VALUES (
                    :id,
                    :username,
                    :email,
                    :plan,
                    :enabled
                )
                RETURNING *
                """)
                .bind("id", user.getId().value())
                .bind("username", user.getUsername().value())
                .bind("email", user.getEmail().value())
                .bind("plan", user.getPlan().name())
                .bind("enabled", user.isEnabled())
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
                    email = :email,
                    plan = :plan,
                    enabled = :enabled,
                    username = :username,
                    violations = :violations,
                    updated_at = NOW()
                WHERE id = :id
                RETURNING *
                """)
                .bind("id", entity.getId())
                .bind("username", entity.getUsername())
                .bind("email", entity.getEmail())
                .bind("plan", entity.getPlan())
                .bind("enabled", entity.isEnabled())
                .bind("violations", entity.getViolations())
                .mapProperties(UserEntity.class)
                .one()
                .map(this.userMapper::toDomain);
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