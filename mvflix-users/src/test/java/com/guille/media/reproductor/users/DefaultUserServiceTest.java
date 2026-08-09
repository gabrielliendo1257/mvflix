package com.guille.media.reproductor.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guille.media.reproductor.users.app.services.DefaultUserService;
import com.guille.media.reproductor.users.domain.exceptions.DowngradeBlockedByUsageException;
import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.UserId;
import com.guille.media.reproductor.users.domain.models.Username;
import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;
import com.guille.media.reproductor.users.domain.ports.StorageUsagePort;
import com.guille.media.reproductor.users.domain.ports.UserService;
import com.guille.media.reproductor.users.infra.db.users.SpringDataUserRepository;
import com.guille.media.reproductor.users.infra.db.users.UserMapper;
import com.guille.media.reproductor.users.infra.db.users.UserMapperImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

@Import({SpringDataUserRepository.class, UserMapperImpl.class, DefaultUserService.class})
class DefaultUserServiceTest extends AbstractR2dbcIntegrationTest {

    @Autowired UserService userService;

    @Autowired UserMapper userMapper;

    @Autowired DatabaseClient databaseClient;

    @Autowired SimpleUserRepository simpleUserRepository;

    @MockitoBean StorageUsagePort storageUsagePort;

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
    void shouldApplyQuotaWithinPlanLimit() {
        long freeQuota = this.user.quota().getUserBytesQuota();

        StepVerifier.create(this.userService.applyQuota("Francis", freeQuota))
                .verifyComplete();
    }

    @Test
    void shouldRejectQuotaAbovePlanLimit() {
        long overQuota = this.user.quota().getUserBytesQuota() + 1;

        StepVerifier.create(this.userService.applyQuota("Francis", overQuota))
                .expectError(com.guille.media.reproductor.users.domain.exceptions.ExceededQuotaException.class)
                .verify();
    }

    @Test
    void shouldRejectQuotaForUnknownUser() {
        StepVerifier.create(this.userService.applyQuota("Ghost", 1L))
                .expectError(com.guille.media.reproductor.users.app.errors.UserNotFoundException.class)
                .verify();
    }

    @Test
    void shouldCreateStorage() {
        var processCreateStorage =
                this.userService.createStorageByNewUsers("Pedro", "pedro@gmail.com");
        StepVerifier.create(processCreateStorage)
                .assertNext(
                        created ->
                                assertEquals("Pedro", created.getUsername().value()))
                .verifyComplete();
    }

    @Test
    void shouldRejectDuplicateUsername() {
        this.userService
                .createStorageByNewUsers("Pedro", "pedro@gmail.com")
                .then(this.userService.createStorageByNewUsers("Pedro", "otro@gmail.com"))
                .as(StepVerifier::create)
                .expectError(
                        com.guille.media.reproductor.users.domain.exceptions
                                .UserAlreadyExistsException.class)
                .verify();
    }

    @Test
    void shouldUpgradePlanImmediately() {
        StepVerifier.create(this.userService.changePlan("Francis", Plan.PRO))
                .assertNext(updated -> assertEquals(Plan.PRO, updated.getPlan()))
                .verifyComplete();
    }

    @Test
    void shouldRejectDowngradeWhenUsageExceedsNewPlanQuota() {
        long freeQuota =
                com.guille.media.reproductor.users.domain.models.StorageQuota.getQuota(Plan.FREE)
                        .getUserBytesQuota();
        org.mockito.Mockito.when(this.storageUsagePort.usedBytesBy("Francis"))
                .thenReturn(Mono.just(freeQuota + 1));

        StepVerifier.create(
                        this.userService
                                .changePlan("Francis", Plan.PRO)
                                .then(this.userService.changePlan("Francis", Plan.FREE)))
                .expectError(DowngradeBlockedByUsageException.class)
                .verify();
    }

    @Test
    void shouldApplyDowngradeWhenUsageFitsNewPlanQuota() {
        org.mockito.Mockito.when(this.storageUsagePort.usedBytesBy("Francis"))
                .thenReturn(Mono.just(10_000L));

        StepVerifier.create(
                        this.userService
                                .changePlan("Francis", Plan.PRO)
                                .then(this.userService.changePlan("Francis", Plan.FREE)))
                .assertNext(updated -> assertEquals(Plan.FREE, updated.getPlan()))
                .verifyComplete();
    }

    @Test
    void shouldKeepCurrentPlanOnNoChange() {
        StepVerifier.create(this.userService.changePlan("Francis", Plan.FREE))
                .assertNext(updated -> assertEquals(Plan.FREE, updated.getPlan()))
                .verifyComplete();
    }

    @Test
    void shouldRejectChangePlanForUnknownUser() {
        StepVerifier.create(this.userService.changePlan("Ghost", Plan.PRO))
                .expectError(com.guille.media.reproductor.users.app.errors.UserNotFoundException.class)
                .verify();
    }
}