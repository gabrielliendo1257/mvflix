package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

/**
 * Identidad confirmada o candidata de una pelicula en un proveedor externo.
 * La metadata puede ser null cuando solo se ha seleccionado el candidato.
 */
public record MovieIdentification(MovieProviderLink providerLink, MovieMetadata metadata) {
    public MovieIdentification {
        if (providerLink == null) {
            throw new IllegalArgumentException("movie provider link is required");
        }
        if (metadata != null && metadata.providerLink() != null
                && !providerLink.equals(metadata.providerLink())) {
            throw new IllegalArgumentException("identification link does not match metadata");
        }
    }

    public static MovieIdentification tmdb(Long tmdbId) {
        if (tmdbId == null) {
            throw new IllegalArgumentException("tmdb id is required");
        }
        return new MovieIdentification(MovieProviderLink.tmdb(ExternalMovieId.of(tmdbId)), null);
    }

    public static MovieIdentification of(MovieMetadata metadata) {
        if (metadata == null || metadata.providerLink() == null) {
            throw new IllegalArgumentException("identified movie metadata is required");
        }
        return new MovieIdentification(metadata.providerLink(), metadata);
    }

    /** Numeric projection for application and HTTP boundaries. */
    public Long tmdbId() {
        return "TMDB".equalsIgnoreCase(this.providerLink.provider())
                ? this.providerLink.externalId().value() : null;
    }
}
