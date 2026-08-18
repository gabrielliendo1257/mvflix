package com.gcorp.service.app.authorizationservice.presenter.dto.request;

/** Credenciales para el token de desarrollo (Postman), solo perfil dev. */
public record DevTokenRequest(String username, String password) {}