package com.guille.media.bff.app.service;

import java.time.Instant;

/** Ticket de stream resuelto: la movie autorizada y el JWT del usuario dueño de la sesión. */
public record StreamTicket(Long movieId, String userJwt, Instant expiresAt) {}