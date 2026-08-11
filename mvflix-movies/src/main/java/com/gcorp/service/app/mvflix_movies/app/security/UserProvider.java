package com.gcorp.service.app.mvflix_movies.app.security;

import reactor.core.publisher.Mono;

public interface UserProvider {

    Mono<AuthenticatedUser> getAuthenticatedUser();
}
