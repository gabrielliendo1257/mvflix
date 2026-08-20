package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Cambio de visibilidad de una pelicula (PUBLIC/PRIVATE/SHARED). {@code usernames}
 * solo aplica a SHARED (lista de usuarios con quien se comparte): se ignora en
 * PUBLIC/PRIVATE y es obligatorio en SHARED.
 */
public record MovieVisibilityRequest(
    @JsonProperty("visibility") String visibility,
    @JsonProperty("usernames") List<String> usernames) {}