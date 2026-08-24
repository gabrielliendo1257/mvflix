package com.guille.media.bff.infrastructure.http;

import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class UsersWebClientAdapter implements UsersWebPort {

  private final WebClient usersWebClient;

  public UsersWebClientAdapter(@Qualifier("usersWebClient") WebClient usersWebClient) {
    this.usersWebClient = usersWebClient;
  }

  @Override
  public Mono<UserProfile> me() {
    return this.usersWebClient
        .get()
        .uri("/api/v1/users/me")
                .retrieve()
        .toEntity(UserProfile.class)
        .mapNotNull(entity -> entity.getBody() == null ? null : entity.getBody());
  }

  @Override
  public Mono<Void> reportViolation(String reason) {
    return this.usersWebClient
        .post()
        .uri("/api/v1/users/me/violations")
                .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("reason", reason))
        .retrieve()
        .toBodilessEntity()
        .then();
  }
}
