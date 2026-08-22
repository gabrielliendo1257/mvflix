package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteMovieRequest(
        @JsonProperty("object_id") @NotNull @Positive Long objectId,
        @JsonProperty("object_key") @NotBlank String objectKey) {}
