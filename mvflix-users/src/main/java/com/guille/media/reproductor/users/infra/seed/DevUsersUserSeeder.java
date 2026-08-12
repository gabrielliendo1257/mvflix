package com.guille.media.reproductor.users.infra.seed;

import com.guille.mvflix.devseed.DevUser;
import com.guille.mvflix.devseed.DevUserSeeder;
import com.guille.media.reproductor.users.domain.exceptions.UserAlreadyExistsException;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.ports.UserService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Seed dev: materializa en mvflix-users a los usuarios definidos en
 * {@code dev-users.yaml} (mismo origen que el IdP y storage).
 */
@Slf4j
@Component
@Profile("dev")
public class DevUsersUserSeeder implements DevUserSeeder {

  private static final Duration TIMEOUT = Duration.ofSeconds(30);

  private final UserService userService;

  public DevUsersUserSeeder(UserService userService) {
    this.userService = userService;
  }

  @Override
  public void seed(DevUser devUser) {
    this.userService
        .createStorageByNewUsers(devUser.getUsername(), devUser.getEmail())
        .onErrorResume(UserAlreadyExistsException.class, error -> Mono.empty())
        .flatMap(
            user -> {
              Plan requested = Plan.valueOf(devUser.getPlan());
              if (user.getPlan() == requested) {
                return Mono.just(user);
              }
              return this.userService.changePlan(user.getUsername().value(), requested);
            })
        .doOnNext(user -> log.info("Dev user provisioned: username={}, plan={}", user.getUsername(), user.getPlan()))
        .block(TIMEOUT);
  }
}