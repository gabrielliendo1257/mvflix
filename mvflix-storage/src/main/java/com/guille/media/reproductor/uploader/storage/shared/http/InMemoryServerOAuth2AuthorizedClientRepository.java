package com.guille.media.reproductor.uploader.storage.shared.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Repositorio en memoria de clientes OAuth2 autorizados para llamadas
 * machine-to-machine ({@code client_credentials}).
 *
 * <p>Reemplaza al {@code UnAuthenticatedServerOAuth2AuthorizedClientRepository}
 * eliminado en Spring Security 6.4: cachea el access token por
 * clientId/principal para no re-autorizar en cada request.
 */
final class InMemoryServerOAuth2AuthorizedClientRepository
        implements ServerOAuth2AuthorizedClientRepository {

    private final Map<String, OAuth2AuthorizedClient> clients = new ConcurrentHashMap<>();

    private static String key(String clientRegistrationId, Authentication authentication) {
        String principal = authentication == null ? "" : authentication.getName();
        return clientRegistrationId + ":" + principal;
    }

    @Override
    public Mono<OAuth2AuthorizedClient> loadAuthorizedClient(
            String clientRegistrationId, Authentication principal, ServerWebExchange exchange) {
        return Mono.justOrEmpty(this.clients.get(key(clientRegistrationId, principal)));
    }

    @Override
    public Mono<Void> saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient, Authentication principal, ServerWebExchange exchange) {
        this.clients.put(key(authorizedClient.getClientRegistration().getRegistrationId(), principal),
                authorizedClient);
        return Mono.empty();
    }

    @Override
    public Mono<Void> removeAuthorizedClient(
            String clientRegistrationId, Authentication principal, ServerWebExchange exchange) {
        this.clients.remove(key(clientRegistrationId, principal));
        return Mono.empty();
    }
}