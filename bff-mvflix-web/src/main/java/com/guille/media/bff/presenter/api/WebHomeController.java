package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.service.WebHomeService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/web", produces = MediaType.APPLICATION_JSON_VALUE)
public class WebHomeController {

  private final WebHomeService webHomeService;

  public WebHomeController(WebHomeService webHomeService) {
    this.webHomeService = webHomeService;
  }

  @GetMapping("/home")
  public Mono<WebHomeService.HomeView> home(
      @RequestHeader(value = "Authorization", required = false) String bearer) {
    return this.webHomeService.home(bearer);
  }
}