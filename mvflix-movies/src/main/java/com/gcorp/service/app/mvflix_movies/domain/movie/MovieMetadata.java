package com.gcorp.service.app.mvflix_movies.domain.movie;

import java.util.List;

public record MovieMetadata(
    String title,
    String originalTitle,
    Integer year,
    List<String> genres,
    Double popularity,
    String duration,
    String director,
    List<String> cast,
    String overview,
    String posterPath,
    String releaseDate,
    String country,
    String language,
    List<String> awards,
    Long tmdbId) {

    /**
     * Metadata minima del flujo de biblioteca: solo se conoce el titulo
     * (derivado del filename); el resto queda null para enriquecer despues.
     */
    public static MovieMetadata onlyTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("movie title is required");
        }
        return new MovieMetadata(
                title, null, null, List.of(), null, null, null,
                List.of(), null, null, null, null, null, List.of(), null);
    }
}
