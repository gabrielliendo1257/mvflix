package com.guille.media.reproductor.users.api.users;

import com.guille.media.reproductor.users.api.dto.response.UserResponse;
import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.MediaIngestionEligibility;
import com.guille.media.reproductor.users.domain.models.Username;
import com.guille.media.reproductor.users.domain.ports.UserService;
import com.guille.media.reproductor.users.infra.security.SecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Import({DefaultUserService.class, SpringDataUserRepository.class})
@WebFluxTest(controllers = UsersPresenter.class)
//@DataR2dbcTest
@AutoConfigureWebTestClient
@Import(SecurityConfig.class)
class UsersPresenterTest {

    @MockitoBean ReactiveJwtDecoder jwtDecoder;
    @MockitoBean UserService defaultUserService;
    @Autowired private WebTestClient webTestClient;
    //@Autowired private SpringDataUserRepository repository;

    @BeforeEach
    void setup() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();

        Mockito.when(jwtDecoder.decode(Mockito.anyString())).thenReturn(Mono.just(jwt));
        User user = User.createNew(new Username("user"), new Email("user@example.com"));
        Mockito.when(defaultUserService.getMe()).thenReturn(Mono.just(user));
    }

    @Test
    void shouldReturnCurrentUser() {
		Jwt jwt = Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject("user")
			.claim("sub", "user")
			.claim("scope", "read")
			.build();
        this.webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt))
				//.mutateWith(SecurityMockServerConfigurers.mock)
                .get()
                .uri("/api/v1/users/me")
				.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
				.accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectBody(UserResponse.class)
                .value(response -> response.username().equals("user"));
    }

    @Test
    void shouldChangePlanWithUsersWriteScope() {
        User proUser = User.createNew(new Username("user"), new Email("user@example.com"));
        proUser.changePlan(Plan.PRO);
        Mockito.when(defaultUserService.changePlan("user", Plan.PRO)).thenReturn(Mono.just(proUser));

        Jwt m2mJwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("backoffice")
                        .claim("sub", "backoffice")
                        .claim("scope", "users.write")
                        .build();

        this.webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(m2mJwt))
                .patch()
                .uri("/api/v1/users/user/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"plan\":\"PRO\"}")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .value(response -> response.plan().equals("PRO"));
    }

    @Test
    void shouldRejectPlanChangeWithoutScope() {
        Jwt userJwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("user")
                        .claim("sub", "user")
                        .build();

        this.webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(userJwt))
                .patch()
                .uri("/api/v1/users/user/plan")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"plan\":\"PRO\"}")
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void shouldReturnPolicyWithMediaIngestionScope() {
        User blockedUser = User.createNew(new Username("blocked"), new Email("blocked@example.com"));
        blockedUser.registerViolation();
        blockedUser.registerViolation();
        blockedUser.registerViolation();
        Mockito.when(defaultUserService.getMediaIngestionEligibility("blocked"))
                .thenReturn(Mono.just(new MediaIngestionEligibility(false)));

        Jwt m2mJwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("media-ingestion")
                        .claim("sub", "media-ingestion")
                        .claim("scope", "media-ingestion")
                        .build();

        this.webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(m2mJwt))
                .get()
                .uri("/api/v1/users/blocked/policy")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(MediaIngestionEligibility.class)
                .value(response -> org.assertj.core.api.Assertions.assertThat(response.allowed()).isFalse());
    }

    @Test
    void shouldRejectPolicyWithoutMediaIngestionScope() {
        Jwt userJwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject("user")
                        .claim("sub", "user")
                        .build();

        this.webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(userJwt))
                .get()
                .uri("/api/v1/users/user/policy")
                .exchange()
                .expectStatus()
                .isForbidden();
    }
}
