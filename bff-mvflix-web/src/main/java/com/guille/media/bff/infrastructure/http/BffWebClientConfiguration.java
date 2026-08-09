package com.guille.media.bff.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class BffWebClientConfiguration {

  @Bean
  WebClient usersWebClient(@Value("${services.users.url}") String usersUrl) {
    return WebClient.builder().baseUrl(usersUrl).build();
  }

  @Bean
  WebClient storageWebClient(@Value("${services.storage.url}") String storageUrl) {
    return WebClient.builder().baseUrl(storageUrl).build();
  }
}