package com.guille.media.bff.infrastructure.http;

import io.netty.channel.ChannelOption;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class BffWebClientConfiguration {

  /**
   * Filtro que inyecta el access token de la sesión OAuth2 del navegador (guardado por
   * {@link ServerOAuth2AuthorizedClientRepository} en la sesión server-side del BFF).
   * Solo con el OAuth2-client activo (perfil no sandbox).
   */
  @Bean
  @Profile("!sandbox")
  ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter(
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    ServerOAuth2AuthorizedClientExchangeFilterFunction filter =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrationRepository, authorizedClientRepository);
    filter.setDefaultClientRegistrationId("movie-app");
    return filter;
  }

  /** En sandbox no hay tokens: filtro no-op para que los WebClient sigan construyendose. */
  @Bean
  @Profile("sandbox")
  ServerOAuth2AuthorizedClientExchangeFilterFunction sandboxOauth2AuthorizedClientFilter() {
    return new ServerOAuth2AuthorizedClientExchangeFilterFunction(
        (clientRegistrationId) -> Mono.empty());
  }

  /**
   * Dev-token: filtro de salida que ramifica segun como se autentico la request entrante.
   * Con Bearer (JwtAuthenticationToken) reenvia ese mismo JWT a los servicios backend; con
   * sesion OAuth2 (navegador) delega en el filtro oauth2-client (token de sesion). Fuera de
   * dev no existe este bean: siempre manda el filtro de sesion.
   */
  @Bean
  @Profile("dev")
  ExchangeFilterFunction devOutboundAuthFilter(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter) {
    return (request, next) ->
        ReactiveSecurityContextHolder.getContext()
            .flatMap(
                context -> {
                  var auth = context.getAuthentication();
                  if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    return next.exchange(
                        ClientRequest.from(request)
                            .headers(
                                headers ->
                                    headers.setBearerAuth(jwtAuth.getToken().getTokenValue()))
                            .build());
                  }
                  return oauth2AuthorizedClientFilter.filter(request, next);
                });
  }

  @Bean
  WebClient usersWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      ObjectProvider<ExchangeFilterFunction> devOutboundAuthFilter,
      @Value("${services.users.url}") String usersUrl,
      @Value("${bff.webclient.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bff.webclient.response-timeout-ms:10000}") long responseTimeoutMs) {
    return this.build(
        usersUrl,
        this.outboundAuthFilter(oauth2AuthorizedClientFilter, devOutboundAuthFilter),
        connectTimeoutMs,
        responseTimeoutMs);
  }

  @Bean
  WebClient storageWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      ObjectProvider<ExchangeFilterFunction> devOutboundAuthFilter,
      @Value("${services.storage.url}") String storageUrl,
      @Value("${bff.webclient.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bff.webclient.response-timeout-ms:10000}") long responseTimeoutMs) {
    return this.build(
        storageUrl,
        this.outboundAuthFilter(oauth2AuthorizedClientFilter, devOutboundAuthFilter),
        connectTimeoutMs,
        responseTimeoutMs);
  }

  @Bean
  WebClient moviesWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      ObjectProvider<ExchangeFilterFunction> devOutboundAuthFilter,
      @Value("${services.movies.url}") String moviesUrl,
      @Value("${bff.webclient.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bff.webclient.response-timeout-ms:10000}") long responseTimeoutMs) {
    return this.build(
        moviesUrl,
        this.outboundAuthFilter(oauth2AuthorizedClientFilter, devOutboundAuthFilter),
        connectTimeoutMs,
        responseTimeoutMs);
  }

  /**
   * En dev el filtro de salida es el compuesto (Bearer o sesion); fuera de dev no hay bean
   * ExchangeFilterFunction propio y manda siempre el filtro oauth2 de sesion.
   */
  private ExchangeFilterFunction outboundAuthFilter(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      ObjectProvider<ExchangeFilterFunction> devOutboundAuthFilter) {
    ExchangeFilterFunction dev = devOutboundAuthFilter.getIfAvailable();
    return dev != null ? dev : oauth2AuthorizedClientFilter;
  }

  private WebClient build(
      String baseUrl,
      ExchangeFilterFunction outboundAuthFilter,
      int connectTimeoutMs,
      long responseTimeoutMs) {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(responseTimeoutMs));
    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .filter(outboundAuthFilter)
        .build();
  }
}
