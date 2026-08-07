package gcorp.microservicesarm.app.mvflix_users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import gcorp.microservicesarm.app.mvflix_users.domain.models.Email;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Plan;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageQuota;
import gcorp.microservicesarm.app.mvflix_users.domain.models.StorageUsage;
import gcorp.microservicesarm.app.mvflix_users.domain.models.User;
import gcorp.microservicesarm.app.mvflix_users.domain.models.UserId;
import gcorp.microservicesarm.app.mvflix_users.domain.models.Username;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.SimpleUserRepository;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.SpringDataUserRepository;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.UserMapper;
import gcorp.microservicesarm.app.mvflix_users.infra.db.users.UserMapperImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({
        SpringDataUserRepository.class,
        UserMapperImpl.class
})
class SpringDataUserRepositoryTest {

    @Autowired
    UserMapper userMapper;

    @Autowired
    SimpleUserRepository simpleUserRepository;

    @Autowired
    DatabaseClient databaseClient;

    private Mono<Void> insertUser(User user) {
        var userEntity = this.userMapper.toEntity(user);
        System.out.println("User entity: " + userEntity);
        return this.simpleUserRepository.save(user).then();
    }

    @BeforeEach
    void setUp() {
        User user = new User(new UserId(UUID.randomUUID()), new Username("Francis"),
                new Email("francis@gmail.com"), Plan.FREE,
                new StorageQuota(100L), new StorageUsage(10L), true);

        databaseClient.sql("DELETE FROM users")
                .fetch()
                .rowsUpdated()
                .then(insertUser(user))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void shouldSaveUser() {
        User user = new User(new UserId(UUID.randomUUID()), new Username("Ejemplo"),
                new Email("ejemplo@gmail.com"), Plan.FREE,
                new StorageQuota(100L), new StorageUsage(10L), true);

        Mono<User> savedUser = this.simpleUserRepository.save(user)
                .doOnNext(saved -> System.out.println("Saved: " + saved.getId().value()))
                .flatMap(userSaved -> this.simpleUserRepository.findById(userSaved.getId()))
                .doOnNext(found -> System.out.println("Found: " + found.getId().value()));

        StepVerifier.create(savedUser)
                .assertNext(found -> {
                    assertEquals(user.getId(), found.getId());
                })
                .verifyComplete();
    }

    @Test
    void shouldFindByName() {
        StepVerifier.create(simpleUserRepository.findByUsername("Francis"))
                .assertNext(saved -> {
                    assertEquals("Francis", saved.getUsername().value());
                })
                .verifyComplete();
    }
}