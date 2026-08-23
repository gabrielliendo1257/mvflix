package com.gcorp.service.app.mvflix_movies.shared.application.security;

public record AuthenticatedUser(String subject, String email) {}
