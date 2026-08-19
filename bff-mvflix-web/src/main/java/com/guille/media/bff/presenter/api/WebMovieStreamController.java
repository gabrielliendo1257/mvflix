package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.dto.StreamTicketDto;
import com.guille.media.bff.app.service.WebMoviesService;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Stream LOCAL con Range: el <video> del front apunta aqui para movies de biblioteca. */
@RestController
public class WebMovieStreamController {

  private final WebMoviesService webMoviesService;

  public WebMovieStreamController(WebMoviesService webMoviesService) {
    this.webMoviesService = webMoviesService;
  }

  /** Ticket de un solo uso por movie: el <video> no puede mandar el JWT en header. */
  @PostMapping("/web/movies/{movieId}/stream-ticket")
  public Mono<StreamTicketDto> streamTicket(@PathVariable Long movieId) {
    return this.webMoviesService.issueStreamTicket(movieId);
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