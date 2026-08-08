package com.guille.media.reproductor.users.app.services;

import com.guille.media.reproductor.users.app.errors.UnauthorizedException;
import com.guille.media.reproductor.users.app.errors.UserNotFoundException;
import com.guille.media.reproductor.users.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.users.domain.models.Email;
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
        return this.simpleUserRepository.save(
                User.createNew(new Username(username), new Email(email)));
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
                                                        new UserNotFoundException(
                                                                "Not exist user by username "
                                                                        + username))));
    }

    @Override
    public Mono<Void> applyQuota(String username, long quotaBytes) {
        return simpleUserRepository
                .findByUsername(username)
                .switchIfEmpty(
                        Mono.error(
                                new UserNotFoundException("Not exist user by username " + username)))
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
}