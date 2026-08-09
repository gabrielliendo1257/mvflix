package com.guille.media.bff.app.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class WebSessionService {

  /**
   * @return sujeto de la sesión OAuth2 del navegador, o vacío si no hay sesión.
   */
  public Mono<String> currentSubject() {
    return ReactiveSecurityContextHolder.getContext()
        .map(context -> context.getAuthentication())
        .filter(Authentication::isAuthenticated)
        .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
        .map(Authentication::getName)
        .switchIfEmpty(Mono.empty());
  }
}