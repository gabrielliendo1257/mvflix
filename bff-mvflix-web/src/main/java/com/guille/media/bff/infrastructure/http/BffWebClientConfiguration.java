package com.guille.media.bff.infrastructure.http;

import io.netty.channel.ChannelOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class BffWebClientConfiguration {

  /**
   * Filtro que inyecta el access token de la sesión OAuth2 del navegador (guardado por
   * {@link ServerOAuth2AuthorizedClientRepository} en la sesión server-side del BFF).
   */
  @Bean
  ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter(
      ReactiveClientRegistrationRepository clientRegistrationRepository,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository) {
    ServerOAuth2AuthorizedClientExchangeFilterFunction filter =
        new ServerOAuth2AuthorizedClientExchangeFilterFunction(
            clientRegistrationRepository, authorizedClientRepository);
    filter.setDefaultClientRegistrationId("movie-app");
    return filter;
  }

  @Bean
  WebClient usersWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      @Value("${services.users.url}") String usersUrl,
      @Value("${bff.webclient.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bff.webclient.response-timeout-ms:10000}") long responseTimeoutMs) {
    return this.build(
        usersUrl, oauth2AuthorizedClientFilter, connectTimeoutMs, responseTimeoutMs);
  }

  @Bean
  WebClient storageWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      @Value("${services.storage.url}") String storageUrl,
      @Value("${bff.webclient.connect-timeout-ms:2000}") int connectTimeoutMs,
      @Value("${bff.webclient.response-timeout-ms:10000}") long responseTimeoutMs) {
    return this.build(
        storageUrl, oauth2AuthorizedClientFilter, connectTimeoutMs, responseTimeoutMs);
  }

  private WebClient build(
      String baseUrl,
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      int connectTimeoutMs,
      long responseTimeoutMs) {
    HttpClient httpClient =
        HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
            .responseTimeout(Duration.ofMillis(responseTimeoutMs));
    return WebClient.builder()
        .baseUrl(baseUrl)
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .filter(oauth2AuthorizedClientFilter)
        .build();
  }
}
