package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.StreamTicketDto;
import com.guille.media.bff.app.service.StreamTicketException;
import com.guille.media.bff.app.service.WebMoviesService;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Stream LOCAL con Range: el <video> del front apunta aqui para movies de biblioteca. */
@Tag(name = "Web · Playback", description = "Tickets HMAC y proxy Range para reproducción")
@RestController
public class WebMovieStreamController {

  private final WebMoviesService webMoviesService;
  private final ServerOAuth2AuthorizedClientRepository authorizedClientRepository;
  private final String clientRegistrationId;

  public WebMovieStreamController(
      WebMoviesService webMoviesService,
      ServerOAuth2AuthorizedClientRepository authorizedClientRepository,
      @Value("${bff.oauth2.client-registration-id:movie-app}") String clientRegistrationId) {
    this.webMoviesService = webMoviesService;
    this.authorizedClientRepository = authorizedClientRepository;
    this.clientRegistrationId = clientRegistrationId;
  }

  /** Ticket de un solo uso por movie: el <video> no puede mandar el JWT en header. */
  @PostMapping("/web/movies/{movieId}/stream-ticket")
  public Mono<StreamTicketDto> streamTicket(@PathVariable Long movieId, ServerWebExchange exchange) {
    return this.resolveUserJwt(exchange)
        .flatMap(userJwt -> this.webMoviesService.issueStreamTicket(movieId, userJwt));
  }

  /**
   * El JWT que viaja dentro del ticket sale del Bearer (dev) o del access token
   * de la sesion OAuth2 del navegador (cookie); ambos los validan los backends.
   */
  private Mono<String> resolveUserJwt(ServerWebExchange exchange) {
    return ReactiveSecurityContextHolder.getContext()
        .flatMap(context -> {
          var auth = context.getAuthentication();
          if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return Mono.just(jwtAuth.getToken().getTokenValue());
          }
          return this.authorizedClientRepository
              .loadAuthorizedClient(this.clientRegistrationId, auth, exchange)
              .switchIfEmpty(Mono.error(new StreamTicketException("Autenticación requerida")))
              .map(client -> client.getAccessToken().getTokenValue());
        });
  }

  @GetMapping("/web/movies/{movieId}/stream")
  public Mono<ResponseEntity<Flux<DataBuffer>>> stream(
      @PathVariable Long movieId,
      @RequestParam(name = "ticket", required = false) String ticket,
      ServerHttpRequest request) {
    return this.webMoviesService.stream(
        movieId, request.getHeaders().getFirst(HttpHeaders.RANGE), ticket);
  }
}