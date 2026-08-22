package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

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
