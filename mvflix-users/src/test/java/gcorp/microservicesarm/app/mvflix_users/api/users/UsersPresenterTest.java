package gcorp.microservicesarm.app.mvflix_users.api.users;

import gcorp.microservicesarm.app.mvflix_users.api.dto.response.UserResponse;
import gcorp.microservicesarm.app.mvflix_users.app.services.DefaultUserService;
import gcorp.microservicesarm.app.mvflix_users.domain.ports.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
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
class UsersPresenterTest {

    @MockitoBean ReactiveJwtDecoder jwtDecoder;
    @MockitoBean UserService defaultUserService;
    @Autowired private WebTestClient webTestClient;
    //@Autowired private SpringDataUserRepository repository;

    @BeforeEach
    void setup() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();

        Mockito.when(jwtDecoder.decode(Mockito.anyString())).thenReturn(Mono.just(jwt));
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
                .expectStatus()
                .isOk()
                .expectBody(UserResponse.class)
                .value(response -> response.username().equals("user"));
    }
}
