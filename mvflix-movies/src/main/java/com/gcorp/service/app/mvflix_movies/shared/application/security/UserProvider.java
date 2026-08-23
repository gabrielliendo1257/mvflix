package com.gcorp.service.app.mvflix_movies.shared.application.security;

import reactor.core.publisher.Mono;

public interface UserProvider {

    Mono<AuthenticatedUser> getAuthenticatedUser();
}
