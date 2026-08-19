package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Cambio de visibilidad de una pelicula (PUBLIC/PRIVATE/SHARED). */
public record MovieVisibilityRequest(String visibility) {}