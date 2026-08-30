package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemVisibility;

import java.util.List;

public record MovieResponse(
    Long id,
    CatalogItemStatus status,
    @JsonProperty("enrichment_status") EnrichmentStatus enrichmentStatus,
    @JsonProperty("object_id") Long objectId,
    CatalogItemVisibility visibility,
    CatalogItemKind kind,
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    Double popularity,
    String duration,
    String director,
    List<String> cast,
    String overview,
    @JsonProperty("poster_path") String posterPath,
    @JsonProperty("release_date") String releaseDate,
    String country,
    String language,
    List<String> awards,
    @JsonProperty("tmdb_id") Long tmdbId) {}
