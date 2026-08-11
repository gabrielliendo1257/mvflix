package com.guille.media.bff.presenter.api;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  /** Propaga el status y el body del servicio aguas abajo (users/storage) sin degradar a 500. */
  @ExceptionHandler(WebClientResponseException.class)
  public Mono<ResponseEntity<String>> downstreamError(WebClientResponseException ex) {
    log.warn("Error {} desde {}: {}", ex.getStatusCode(), ex.getRequest().getURI(), ex.getMessage());
    String body = ex.getResponseBodyAsString();
    if (body == null || body.isBlank()) {
      body = "{\"code\":" + ex.getStatusCode().value() + ",\"error\":\"DOWNSTREAM_ERROR\"}";
    }
    MediaType contentType = ex.getHeaders().getContentType();
    if (contentType == null) {
      contentType = MediaType.APPLICATION_JSON;
    }
    return Mono.just(
        ResponseEntity.status(ex.getStatusCode()).contentType(contentType).body(body));
  }
}
