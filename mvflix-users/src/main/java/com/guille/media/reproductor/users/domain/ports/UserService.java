package com.guille.media.reproductor.users.domain.ports;

import com.guille.media.reproductor.users.domain.models.User;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<Void> reserveStorage(long bytes, String userId);

	Mono<User> createStorageByNewUsers(String username, String email);

	Mono<User> getMe();
}
