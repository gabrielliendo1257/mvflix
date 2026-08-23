package com.guille.media.reproductor.uploader.storage.shared.security;

import reactor.core.publisher.Mono;

public interface UserProvider {
    Mono<AuthenticatedUser> getAuthenticatedUser();
}
