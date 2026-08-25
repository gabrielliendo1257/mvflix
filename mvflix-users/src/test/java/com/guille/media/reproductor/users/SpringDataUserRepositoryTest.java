package com.guille.media.reproductor.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.UserId;
import com.guille.media.reproductor.users.domain.models.Username;
import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;
import com.guille.media.reproductor.users.infra.db.users.SpringDataUserRepository;
import com.guille.media.reproductor.users.infra.db.users.UserMapper;
import com.guille.media.reproductor.users.infra.db.users.UserMapperImpl;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Import({
        SpringDataUserRepository.class,
        UserMapperImpl.class
})
class SpringDataUserRepositoryTest extends AbstractR2dbcIntegrationTest {

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
                new Email("francis@gmail.com"), Plan.FREE, true, 0, null, null);

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
                new Email("ejemplo@gmail.com"), Plan.FREE, true, 0, null, null);

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