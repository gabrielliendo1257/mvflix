package com.guille.media.reproductor.users.api.users;

import com.guille.media.reproductor.users.api.dto.request.UserData;
import com.guille.media.reproductor.users.api.dto.request.PlanData;
import com.guille.media.reproductor.users.api.dto.request.ViolationRequest;
import com.guille.media.reproductor.users.api.dto.response.UserResponse;
import com.guille.media.reproductor.users.domain.ports.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@Slf4j
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class UsersPresenter {

    private final UserService userService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> createNewStorage(@RequestBody UserData userData) {
        return this.userService
                .createStorageByNewUsers(userData.username(), userData.email())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)));
    }

    @GetMapping(value = "/me")
    public Mono<ResponseEntity<?>> me() {
        return this.userService.getMe().map(user -> ResponseEntity.ok(UserResponse.from(user)));
    }

    /**
     * Registra una infracción de subida del usuario autenticado (lo invoca el BFF con el token
     * de sesión del infractor). Al llegar al umbral el usuario queda bloqueado para subidas.
     */
    @PostMapping(value = "/me/violations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> reportViolation(@RequestBody ViolationRequest request) {
        return this.userService
                .getMe()
                .flatMap(user -> this.userService.registerViolation(user.getUsername().value(),
                        request.reason()))
                .map(user -> ResponseEntity.ok(UserResponse.from(user)));
    }

    /**
     * Cambio de plan (contrato M2M, scope {@code users.write}). La cuota efectiva la
     * aplica el storage-service (fuente de verdad del límite y el consumo).
     */
    @PatchMapping(value = "/{username}/plan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> changePlan(@PathVariable String username,
            @RequestBody PlanData planData) {
        return this.userService
                .changePlan(username, planData.plan())
                .map(user -> ResponseEntity.ok(UserResponse.from(user)));
    }
}
