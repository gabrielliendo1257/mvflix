package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.service.WebSessionService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping(path = "/web", produces = MediaType.APPLICATION_JSON_VALUE)
public class WebSessionController {

  private final WebSessionService webSessionService;

  public WebSessionController(WebSessionService webSessionService) {
    this.webSessionService = webSessionService;
  }

  @GetMapping("/session")
  public Mono<Map<String, Object>> session() {
    return this.webSessionService
        .currentSubject()
        .map(subject -> Map.<String, Object>of("authenticated", true, "subject", subject))
        .defaultIfEmpty(Map.of("authenticated", false));
  }
}