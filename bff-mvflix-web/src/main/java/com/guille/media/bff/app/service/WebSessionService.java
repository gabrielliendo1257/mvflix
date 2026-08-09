package com.guille.media.bff.app.service;

import com.guille.media.bff.app.ports.UsersWebPort;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WebSessionService {

  private final UsersWebPort usersWebClient;

  public Mono<String> subject(String bearer) {
    if (bearer == null || bearer.isBlank()) {
      return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token"));
    }
    return this.usersWebClient
        .me(bearer)
        .map(UserProfileDto -> UserProfileDto.username())
        .switchIfEmpty(
            Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token")));
  }
}