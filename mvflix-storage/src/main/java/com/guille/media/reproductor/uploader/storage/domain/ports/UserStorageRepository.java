package com.guille.media.reproductor.uploader.storage.domain.ports;

import com.guille.media.reproductor.uploader.storage.domain.models.UserStorage;
import reactor.core.publisher.Mono;

public interface UserStorageRepository {
	Mono<UserStorage> save(UserStorage user);
	Mono<UserStorage> findById(Long userStorageId);
	Mono<UserStorage> findByOwnerUsername(String ownerUsername);
}
