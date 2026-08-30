package com.guille.media.reproductor.users.app.services;

import com.guille.media.reproductor.users.app.errors.UnauthorizedException;
import com.guille.media.reproductor.users.app.errors.UserNotFoundException;
import com.guille.media.reproductor.users.domain.exceptions.UserAlreadyExistsException;
import com.guille.media.reproductor.users.domain.models.BillingCycle;
import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.PlanChangeDecision;
import com.guille.media.reproductor.users.domain.models.MediaIngestionEligibility;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.Username;
import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;
import com.guille.media.reproductor.users.domain.ports.UserService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DefaultUserService implements UserService {

  private final SimpleUserRepository simpleUserRepository;

  public DefaultUserService(SimpleUserRepository simpleUserRepository) {
    this.simpleUserRepository = simpleUserRepository;
  }

  @Override
  public Mono<User> createStorageByNewUsers(String username, String email) {
    return simpleUserRepository
        .findByUsername(username)
        .hasElement()
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new UserAlreadyExistsException("Not exist user by username " + username));
              }
              return simpleUserRepository.save(
                  User.createNew(new Username(username), new Email(email)));
            });
  }

  @Override
  public Mono<User> getMe() {
    return ReactiveSecurityContextHolder.getContext()
        .map(ctx -> ctx.getAuthentication().getName())
        .switchIfEmpty(Mono.error(new UnauthorizedException("Authentication is null")))
        .flatMap(
            username ->
                simpleUserRepository
                    .findByUsername(username)
                    .switchIfEmpty(
                        Mono.error(
                            new UserNotFoundException("Not exist user by username " + username))));
  }

  @Override
  public Mono<User> changePlan(String username, Plan requested) {
    return getByUsername(username)
        .flatMap(
            user -> {
              PlanChangeDecision decision =
                  PlanChangeDecision.evaluate(user.getPlan(), requested);
              if (decision == PlanChangeDecision.NO_CHANGE) {
                return Mono.just(user);
              }
              log.info("Cambio de plan para {}: {} -> {}", username, user.getPlan(), requested);
              user.changePlan(requested);
              return this.simpleUserRepository.update(user);
            });
  }

  @Override
  public Mono<User> registerViolation(String username, String reason) {
    return getByUsername(username)
        .flatMap(
            user -> {
              boolean blocked = user.registerViolation();
              log.warn(
                  "Violación registrada para {}: {} (total={}, bloqueado={})",
                  username, reason, user.getViolations(), blocked);
              return this.simpleUserRepository
                  .update(user)
                  .doOnNext(
                      updated -> {
                        if (blocked) {
                          log.error(
                              "Usuario {} BLOQUEADO: alcanzó {} violaciones",
                              username,
                              User.VIOLATION_THRESHOLD);
                        }
                      });
            });
  }

  @Override
  public Mono<User> getByUsername(String username) {
    return this.simpleUserRepository
        .findByUsername(username)
        .switchIfEmpty(
            Mono.error(new UserNotFoundException("Not exist user by username " + username)));
  }

  @Override
  public Mono<MediaIngestionEligibility> getMediaIngestionEligibility(String username) {
    return getByUsername(username).map(MediaIngestionEligibility::from);
  }
}
