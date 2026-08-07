package com.guille.media.reproductor.uploader.storage.app.security;

import reactor.core.publisher.Mono;

public interface UserProvider {
    Mono<AuthenticatedUser> getAuthenticatedUser();
}
