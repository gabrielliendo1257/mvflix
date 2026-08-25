package com.guille.media.bff.app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserProfile(
    String id,
    String username,
    String displayName,
    String avatarUrl,
    String email,
    String plan,
    boolean enabled,
    int violations,
    boolean blocked) {}
