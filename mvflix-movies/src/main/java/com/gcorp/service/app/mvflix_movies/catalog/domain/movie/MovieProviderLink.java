package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/** Link between a movie and its identity at an external provider. */
public record MovieProviderLink(String provider, ExternalMovieId externalId) {
    public MovieProviderLink {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("movie provider is required");
        }
        if (externalId == null) {
            throw new IllegalArgumentException("external movie id is required");
        }
    }

    public static MovieProviderLink tmdb(ExternalMovieId id) {
        return new MovieProviderLink("TMDB", id);
    }
}
