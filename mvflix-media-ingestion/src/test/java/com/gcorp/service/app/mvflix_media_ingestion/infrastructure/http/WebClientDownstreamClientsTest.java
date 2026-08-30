package com.gcorp.service.app.mvflix_media_ingestion.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class WebClientDownstreamClientsTest {
  @Test
  void requestUploadCompletionUsesStorageEndpointAndHeaders() {
    ExchangeFunction exchange = mock(ExchangeFunction.class);
    var request = new ClientRequest[1];
    when(exchange.exchange(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
      request[0] = invocation.getArgument(0);
      return Mono.just(ClientResponse.create(HttpStatus.ACCEPTED).build());
    });
    var builder = WebClient.builder().exchangeFunction(exchange);
    var clients = new WebClientDownstreamClients("http://movies", "http://storage", builder);

    clients.requestUploadCompletion("42", "pepe", "ingestion:complete-upload").block();

    assertThat(request[0].method().name()).isEqualTo("POST");
    assertThat(request[0].url()).isEqualTo(URI.create("http://storage/api/v1/movie/storage/upload/42/complete"));
    assertThat(request[0].headers().getFirst("X-Actor-Id")).isEqualTo("pepe");
    assertThat(request[0].headers().getFirst("Idempotency-Key")).isEqualTo("ingestion:complete-upload");
  }
}
