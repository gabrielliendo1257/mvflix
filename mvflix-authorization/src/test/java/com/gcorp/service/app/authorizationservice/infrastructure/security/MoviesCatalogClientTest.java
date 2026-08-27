package com.gcorp.service.app.authorizationservice.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * El machine-client de limpieza MANAGED se registra con client_credentials y
 * un scope dedicado storage.objects.delete. NO reutiliza storage.stream
 * (eliminar ≠ reproducir) ni el secret del usuario.
 */
class MoviesCatalogClientTest {

    @Test
    void registersClientCredentialsWithDedicatedDeleteScope() {
        RegisteredClient client = OAuth2AuthorizationConfig.moviesCatalogClient(
                new FakeEncoder(), "secret-from-env");

        assertThat(client.getId()).isEqualTo("movies-catalog-app");
        assertThat(client.getClientId()).isEqualTo("movies-catalog");
        assertThat(client.getAuthorizationGrantTypes())
                .containsOnly(AuthorizationGrantType.CLIENT_CREDENTIALS);
        assertThat(client.getClientAuthenticationMethods())
                .contains(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        // Scope dedicado y exclusivo: borrar no es reproducir.
        assertThat(client.getScopes()).containsOnly("storage.objects.delete");
        assertThat(client.getScopes()).doesNotContain("storage.stream");
        // El secret se guarda codificado, nunca en claro.
        assertThat(client.getClientSecret()).isNotEqualTo("secret-from-env");
        assertThat(client.getClientSecret()).isEqualTo("hash(secret-from-env)");
    }

    private static final class FakeEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword) {
            return "hash(" + rawPassword + ")";
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals("hash(" + rawPassword + ")");
        }
    }
}
