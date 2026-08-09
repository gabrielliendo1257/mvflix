package com.guille.media.reproductor.users.domain.ports;

import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.User;

import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> createStorageByNewUsers(String username, String email);

    Mono<User> getMe();

    Mono<Void> applyQuota(String username, long quotaBytes);

    /**
     * Cambia el plan de {@code username}. Aplica la política de facturación: el
     * upgrade es inmediato; el downgrade consulta el uso real al storage-service
     * y se rechaza si excede la cuota del plan pedido ({@code DowngradeBlockedByUsageException}).
     */
    Mono<User> changePlan(String username, Plan requested);
}