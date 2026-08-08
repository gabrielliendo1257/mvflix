package com.guille.media.reproductor.users.infra.db.users;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface UserR2dbcRepository extends ReactiveCrudRepository<UserEntity, UUID> {
    
    @Query("select id, email from users c where d.email = $1")
    Flux<UserEntity> findByEmail(String email);
}
