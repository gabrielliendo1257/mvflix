package com.gcorp.service.app.authorizationservice.presenter.dto.response;

/** Token de acceso para desarrollo, misma forma que el del flujo OAuth2. */
public record DevTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String scope) {}