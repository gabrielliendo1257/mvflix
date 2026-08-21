package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Item de la grilla del catálogo: lo mínimo que renderiza una tarjeta. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovieListItemDto(
    Long id,
    String status,
    String visibility,
    String kind,
    String title,
    Integer year,
    @JsonProperty("poster_path") String posterPath) {}