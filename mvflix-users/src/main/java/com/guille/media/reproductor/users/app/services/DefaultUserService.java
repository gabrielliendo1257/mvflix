package com.guille.media.reproductor.users.app.services;

import com.guille.media.reproductor.users.app.errors.UnauthorizedException;
import com.guille.media.reproductor.users.app.errors.UserNotFoundException;
import com.guille.media.reproductor.users.domain.models.Email;
import com.guille.media.reproductor.users.domain.models.User;
import com.guille.media.reproductor.users.domain.models.UserId;
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
    public Mono<Void> reserveStorage(long bytes, String userId) {
        return this.simpleUserRepository
                .findById(UserId.from(userId))
                .switchIfEmpty(
                        Mono.error(
                                new UserNotFoundException(
                                        "Usuario con id " + userId + " no existe.")))
                .doOnNext(
                        user -> {
                            log.info("Apply consume by: {}", user.getId());
                            user.consumeStorage(bytes);
                        })
                .flatMap(this.simpleUserRepository::update)
                .then();
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
}
