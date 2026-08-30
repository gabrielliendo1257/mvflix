package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.http;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfiguration {

  @Bean
  ReactiveOAuth2AuthorizedClientManager reactiveOAuth2AuthorizedClientManager(
      ReactiveClientRegistrationRepository registrations) {
    var service = new InMemoryReactiveOAuth2AuthorizedClientService(registrations);
    var manager =
        new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(registrations, service);
    manager.setAuthorizedClientProvider(
        ReactiveOAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
    return manager;
  }
}
