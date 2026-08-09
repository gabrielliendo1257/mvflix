package com.guille.media.reproductor.users.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.guille.media.reproductor.users.domain.models.Plan;

/** Petición de cambio de plan. */
public record PlanData(@JsonProperty(value = "plan") Plan plan) {

    @JsonCreator
    public PlanData {}
}