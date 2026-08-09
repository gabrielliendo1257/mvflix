package com.guille.media.bff.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

import org.springframework.web.reactive.function.client.WebClient;

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
      @Value("${services.users.url}") String usersUrl) {
    return WebClient.builder().baseUrl(usersUrl).filter(oauth2AuthorizedClientFilter).build();
  }

  @Bean
  WebClient storageWebClient(
      ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2AuthorizedClientFilter,
      @Value("${services.storage.url}") String storageUrl) {
    return WebClient.builder().baseUrl(storageUrl).filter(oauth2AuthorizedClientFilter).build();
  }
}