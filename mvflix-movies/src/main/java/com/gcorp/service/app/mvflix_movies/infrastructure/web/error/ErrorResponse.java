package com.gcorp.service.app.mvflix_movies.infrastructure.web.error;

public record ErrorResponse(Integer code, String error, String message) {}
