package com.guille.media.reproductor.users.domain.ports;

import com.guille.media.reproductor.users.domain.models.Plan;
import com.guille.media.reproductor.users.domain.models.MediaIngestionEligibility;
import com.guille.media.reproductor.users.domain.models.User;

import reactor.core.publisher.Mono;

public interface UserService {
    Mono<User> createStorageByNewUsers(String username, String email);

    Mono<User> getMe();

    Mono<User> getByUsername(String username);

    Mono<MediaIngestionEligibility> getMediaIngestionEligibility(String username);

    /**
     * Cambia el plan de {@code username} según la política de facturación
     * ({@code PlanChangeDecision}). La cuota efectiva la aplica el storage-service,
     * fuente de verdad del límite y del consumo del usuario.
     */
    Mono<User> changePlan(String username, Plan requested);

    /**
     * Registra una infracción de subida para {@code username}. Al alcanzar el umbral
     * ({@link User#VIOLATION_THRESHOLD}) el usuario queda bloqueado.
     */
    Mono<User> registerViolation(String username, String reason);
}
