package com.gcorp.service.app.authorizationservice.presenter.dto.request;

public record AuthCodeReceiver(String code, String codeVerifier) {
}
