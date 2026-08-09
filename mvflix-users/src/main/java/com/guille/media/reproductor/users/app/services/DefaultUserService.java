package com.guille.media.reproductor.users.app.services;

import com.guille.media.reproductor.users.app.errors.UnauthorizedException;
import com.guille.media.reproductor.users.app.errors.UserNotFoundException;
import com.guille.media.reproductor.users.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.users.domain.exceptions.UserAlreadyExistsException;
import com.guille.media.reproductor.users.domain.models.BillingCycle;
import com.guille.media.reproductor.users.domain.models.DowngradePolicy;
import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.PlanChangeDecision;
import com.guille.media.reproductor.users.domain.models.StorageQuota;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.Username;
import com.guille.media.reproductor.users.domain.ports.SimpleUserRepository;
import com.guille.media.reproductor.users.domain.ports.StorageUsagePort;
import com.guille.media.reproductor.users.domain.ports.UserService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DefaultUserService implements UserService {

  private final SimpleUserRepository simpleUserRepository;
  private final StorageUsagePort storageUsagePort;

  public DefaultUserService(
      SimpleUserRepository simpleUserRepository, StorageUsagePort storageUsagePort) {
    this.simpleUserRepository = simpleUserRepository;
    this.storageUsagePort = storageUsagePort;
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
  public Mono<Void> applyQuota(String username, long quotaBytes) {
    return getByUsername(username)
        .flatMap(
            user -> {
              if (user.quota().isExceeded(quotaBytes)) {
                return Mono.error(
                    new ExceededQuotaException(
                        "Quota "
                            + quotaBytes
                            + " exceeds the plan limit "
                            + user.quota().getUserBytesQuota()
                            + " for user "
                            + username));
              }
              return Mono.empty();
            });
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
              if (decision == PlanChangeDecision.UPGRADE_IMMEDIATE) {
                log.info("Upgrade inmediato a {} para {}", requested, username);
                user.changePlan(requested);
                return this.simpleUserRepository.update(user);
              }
              return this.storageUsagePort
                  .usedBytesBy(username)
                  .flatMap(
                      usedBytes -> {
                        DowngradePolicy.evaluate(user.getPlan(), requested, usedBytes);
                        log.info(
                            "Downgrade a {} para {} con uso real {} bytes: se aplica al fin de ciclo",
                            requested,
                            username,
                            usedBytes);
                        user.changePlan(requested);
                        return this.simpleUserRepository.update(user);
                      });
            });
  }

  private Mono<User> getByUsername(String username) {
    return this.simpleUserRepository
        .findByUsername(username)
        .switchIfEmpty(
            Mono.error(new UserNotFoundException("Not exist user by username " + username)));
  }
}
