package com.guille.media.bff.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OutboundWebClientWiringTest.TestConfiguration.class)
class OutboundWebClientWiringTest {

  private static final AtomicReference<URI> MOVIES_REQUEST = new AtomicReference<>();
  private static final AtomicReference<URI> STORAGE_REQUEST = new AtomicReference<>();
  private static final AtomicReference<URI> USERS_REQUEST = new AtomicReference<>();

  @Autowired
  private MoviesWebClientAdapter moviesAdapter;

  @Autowired
  private StorageWebClientAdapter storageAdapter;

  @Autowired
  private UsersWebClientAdapter usersAdapter;

  @BeforeEach
  void clearCapturedRequests() {
    MOVIES_REQUEST.set(null);
    STORAGE_REQUEST.set(null);
    USERS_REQUEST.set(null);
  }

  @Test
  void shouldRouteEachAdapterThroughItsServiceSpecificWebClient() {
    this.moviesAdapter.searchCandidates("braveheart", null)
        .collectList()
        .block(Duration.ofSeconds(1));
    this.storageAdapter.listUploads(10)
        .collectList()
        .block(Duration.ofSeconds(1));
    this.usersAdapter.me()
        .block(Duration.ofSeconds(1));

    assertThat(MOVIES_REQUEST.get()).hasHost("movies.test");
    assertThat(STORAGE_REQUEST.get()).hasHost("storage.test");
    assertThat(USERS_REQUEST.get()).hasHost("users.test");
  }

  @Configuration(proxyBeanMethods = false)
  @Import({
      MoviesWebClientAdapter.class,
      StorageWebClientAdapter.class,
      UsersWebClientAdapter.class
  })
  static class TestConfiguration {

    @Bean("moviesWebClient")
    WebClient moviesWebClient() {
      return webClient("http://movies.test", MOVIES_REQUEST, "[]");
    }

    @Bean("storageWebClient")
    WebClient storageWebClient() {
      return webClient("http://storage.test", STORAGE_REQUEST, "[]");
    }

    @Bean("usersWebClient")
    WebClient usersWebClient() {
      return webClient(
          "http://users.test",
          USERS_REQUEST,
          "{\"id\":\"user-1\",\"username\":\"Admin\",\"enabled\":true}");
    }

    private static WebClient webClient(
        String baseUrl, AtomicReference<URI> capturedRequest, String responseBody) {
      return WebClient.builder()
          .baseUrl(baseUrl)
          .exchangeFunction(request -> {
            capturedRequest.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(responseBody)
                .build());
          })
          .build();
    }
  }
}
