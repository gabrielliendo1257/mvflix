package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UsersWebClientAdapter implements UsersWebPort {

  private final WebClient usersWebClient;

  @Override
  public Mono<UserProfile> me(String bearer) {
    return this.usersWebClient
        .get()
        .uri("/api/v1/users/me")
        .header(HttpHeaders.AUTHORIZATION, bearer)
        .retrieve()
        .toEntity(UserProfile.class)
        .mapNotNull(entity -> entity.getBody() == null ? null : entity.getBody());
  }
}