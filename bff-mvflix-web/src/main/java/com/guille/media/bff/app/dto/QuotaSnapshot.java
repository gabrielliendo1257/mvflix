package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuotaSnapshot(
    String ownerUsername, long quotaBytes, long usedBytes, long remainingBytes) {}