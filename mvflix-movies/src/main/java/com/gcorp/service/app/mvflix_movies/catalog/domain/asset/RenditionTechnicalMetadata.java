package com.gcorp.service.app.mvflix_movies.catalog.domain.asset;

/** Optional facts produced while inspecting or transcoding a rendition. */
public record RenditionTechnicalMetadata(
        String filename,
        Long duration,
        String container,
        String videoCodec,
        String resolution) {

    public RenditionTechnicalMetadata {
        if (duration != null && duration < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }
    }
}
