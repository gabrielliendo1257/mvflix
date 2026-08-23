package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.service.StreamTicketException;
import com.guille.media.bff.experience.addmedia.application.UploadOrchestrationException;
import com.guille.media.bff.experience.addmedia.application.IdempotencyConflictException;
import com.guille.media.bff.shared.error.EntityNotFound;
import com.guille.media.bff.experience.addmedia.model.InvalidAddMediaTransition;
import com.guille.media.bff.presenter.api.dto.OrchestrationError;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  /** Ticket de stream inválido/expirado: el <video> debe pedir uno nuevo. */
  @ExceptionHandler(StreamTicketException.class)
  public Mono<ResponseEntity<OrchestrationError>> streamTicket(StreamTicketException ex) {
    log.warn("Stream ticket rechazado: {}", ex.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OrchestrationError(
                HttpStatus.UNAUTHORIZED.value(), "STREAM_TICKET_INVALID", ex.getMessage())));
  }

  /** Veredicto del flujo orquestado: el BFF ya ejecutó rollback/penalidad y diagnostica. */
  @ExceptionHandler(UploadOrchestrationException.class)
  public Mono<ResponseEntity<OrchestrationError>> orchestration(
      UploadOrchestrationException ex) {
    log.warn("Orquestación fallida: code={} message={}", ex.getCode(), ex.getMessage());
    return Mono.just(
        ResponseEntity.status(ex.getStatus())
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OrchestrationError(ex.getStatus().value(), ex.getCode(), ex.getMessage())));
  }

  /** Recurso de aplicación inexistente (p.ej. proceso Add Media ajeno). */
  @ExceptionHandler(EntityNotFound.class)
  public Mono<ResponseEntity<OrchestrationError>> entityNotFound(EntityNotFound ex) {
    log.warn("Recurso no encontrado: {}", ex.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OrchestrationError(
                HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage())));
  }

  /** Misma idempotencyKey con payload distinto: conflicto, no silencio. */
  @ExceptionHandler(IdempotencyConflictException.class)
  public Mono<ResponseEntity<OrchestrationError>> idempotencyConflict(
      IdempotencyConflictException ex) {
    log.warn("Conflicto de idempotencia: {}", ex.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OrchestrationError(
                HttpStatus.CONFLICT.value(), ex.getCode(), ex.getMessage())));
  }

  /** Fase del proceso incompatible con la operación pedida. */
  @ExceptionHandler(InvalidAddMediaTransition.class)
  public Mono<ResponseEntity<OrchestrationError>> invalidTransition(
      InvalidAddMediaTransition ex) {
    log.warn("Add Media transición inválida: {}", ex.getMessage());
    return Mono.just(
        ResponseEntity.status(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new OrchestrationError(
                HttpStatus.CONFLICT.value(), "INVALID_ADD_MEDIA_TRANSITION", ex.getMessage())));
  }

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
