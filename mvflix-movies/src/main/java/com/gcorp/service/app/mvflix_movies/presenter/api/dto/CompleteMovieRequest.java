package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompleteMovieRequest(
        @JsonProperty("object_id") Long objectId,
        @JsonProperty("object_key") String objectKey) {}
