package gcorp.microservicesarm.app.mvflix_users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gcorp.microservicesarm.app.mvflix_users.app.errors.ExceededQuotaException;
import gcorp.microservicesarm.app.mvflix_users.app.errors.InvalidUserIdException;
import gcorp.microservicesarm.app.mvflix_users.app.errors.UserNotFoundException;
import gcorp.microservicesarm.app.mvflix_users.app.services.DefaultUserService;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Email;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Plan;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageQuota;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageUsage;
import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Username;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.SimpleUserRepository;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.UserService;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.SpringDataUserRepository;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.UserMapper;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.UserMapperImpl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

@Slf4j
@DataR2dbcTest
@Import({SpringDataUserRepository.class, UserMapperImpl.class, DefaultUserService.class})
class DefaultUserServiceTest {

    @Autowired UserService userService;

    @Autowired UserMapper userMapper;

    @Autowired DatabaseClient databaseClient;

    @Autowired SimpleUserRepository simpleUserRepository;

    User user = null;

    private Mono<Void> insertUser(User user) {
        var userEntity = this.userMapper.toEntity(user);
        System.out.println("User entity: " + userEntity);
        return this.simpleUserRepository.save(user).then();
    }

    @BeforeEach
    void setUp() {
        this.user =
                new User(
                        new UserId(UUID.randomUUID()),
                        new Username("Francis"),
                        new Email("francis@gmail.com"),
                        Plan.FREE,
                        new StorageQuota(100L),
                        new StorageUsage(10L),
                        true);

        databaseClient
                .sql("DELETE FROM users")
                .fetch()
                .rowsUpdated()
                .then(insertUser(user))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @AfterEach
    void setDown() {
        this.databaseClient.sql("DELETE FROM users").fetch().rowsUpdated().then();
    }

    @Test
    @WithMockUser(username = "Francis")
    void shouldGetMe() {
        var processGetMe = this.userService.getMe();

        StepVerifier.create(processGetMe)
                .assertNext(
                        user -> {
                            assertEquals(
                                    this.user.getUsername().value(), user.getUsername().value());
                        })
                .verifyComplete();
    }

    @Test
    void shouldReserveStorageWithExceededQuotaException() {
        System.out.println("Current user: " + this.user);
        var processReserveStorage =
                this.userService.reserveStorage(100L, this.user.getId().value().toString());

        StepVerifier.create(processReserveStorage)
                .expectError(ExceededQuotaException.class)
                .verify();
    }

    @Test
    void shouldReserveStorageWithUserNotFoundException() {
        var processReserveStorage =
                this.userService.reserveStorage(100L, UUID.randomUUID().toString());
        StepVerifier.create(processReserveStorage)
                .expectError(UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldReserveStorageWithInvalidUserIdException() {
        try {
            var processReserveStorage = this.userService.reserveStorage(100L, "invalid_id");
            StepVerifier.create(processReserveStorage).verifyComplete();
        } catch (InvalidUserIdException ex) {
            log.info("InvalidUserIdException: {}", ex.getMessage());
        }
    }

    @Test
    void shouldCreateStorage() {
        var processCreateStorage =
                this.userService.createStorageByNewUsers("Pedro", "pedro@gmail.com");
        StepVerifier.create(processCreateStorage).verifyComplete();
    }
}
