package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Reemplazo de la lista de usuarios con quienes se comparte una pelicula. */
public record MovieSharesRequest(@JsonProperty("usernames") List<String> usernames) {}