package com.gcorp.service.app.mvflix_movies.catalog.domain.metadata;

import java.util.List;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.ExternalMovieId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieProviderLink;

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
    MovieProviderLink providerLink) implements CatalogMetadata {

    /** Legacy construction boundary; the domain stores a typed provider link. */
    public MovieMetadata(String title, String originalTitle, Integer year, List<String> genres,
            Double popularity, String duration, String director, List<String> cast, String overview,
            String posterPath, String releaseDate, String country, String language, List<String> awards,
            Object legacyProviderId) {
        this(title, originalTitle, year, genres, popularity, duration, director, cast, overview,
                posterPath, releaseDate, country, language, awards,
                legacyProviderId instanceof MovieProviderLink link
                        ? link
                        : legacyProviderId == null
                                ? null
                                : MovieProviderLink.tmdb(ExternalMovieId.of((Long) legacyProviderId)));
    }

    /** Numeric compatibility projection used only by existing application ports. */
    public Long tmdbId() {
        return this.providerLink == null ? null : this.providerLink.externalId().value();
    }

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

    /**
     * Copia identificada: fija el identificador estable del proveedor sin
     * alterar el resto de la metadata aportada por el usuario o el preview.
     */
    public MovieMetadata withTmdbId(Long tmdbId) {
        return new MovieMetadata(
                this.title,
                this.originalTitle,
                this.year,
                this.genres,
                this.popularity,
                this.duration,
                this.director,
                this.cast,
                this.overview,
                this.posterPath,
                this.releaseDate,
                this.country,
                this.language,
                this.awards,
                tmdbId == null ? null : MovieProviderLink.tmdb(ExternalMovieId.of(tmdbId)));
    }

    /**
     * Desvincula la metadata del proveedor externo: limpia tmdbId, posterPath y
     * popularity (los datos que solo aporta TMDB) y conserva lo que el usuario
     * haya rellenado a mano. Sirve para media que no representa una pelicula.
     */
    public MovieMetadata withoutProvider() {
        return new MovieMetadata(
                this.title,
                this.originalTitle,
                this.year,
                this.genres,
                null,
                this.duration,
                this.director,
                this.cast,
                this.overview,
                null,
                this.releaseDate,
                this.country,
                this.language,
                this.awards,
                null);
    }
}
