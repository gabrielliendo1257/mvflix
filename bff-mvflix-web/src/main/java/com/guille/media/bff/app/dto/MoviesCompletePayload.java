package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Payload interno hacia mvflix-movies para transicionar la película a READY. */
public record MoviesCompletePayload(
    @JsonProperty("object_id") Long objectId,
    @JsonProperty("object_key") String objectKey) {}
