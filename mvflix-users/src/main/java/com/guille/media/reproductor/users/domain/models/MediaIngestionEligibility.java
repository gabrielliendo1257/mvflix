package com.guille.media.reproductor.users.domain.models;

/** Minimal policy projection consumed by Media Ingestion. */
public record MediaIngestionEligibility(boolean allowed) {
  public static MediaIngestionEligibility from(User user) {
    return new MediaIngestionEligibility(user.isEnabled() && !user.isBlocked());
  }
}
