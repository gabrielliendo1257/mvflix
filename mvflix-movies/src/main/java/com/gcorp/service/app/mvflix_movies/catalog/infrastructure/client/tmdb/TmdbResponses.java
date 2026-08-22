package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** DTOs crudos de la API de TMDB (solo lo que el catalogo necesita). */
record TmdbResponses() {

    record SearchResponse(List<MovieSummary> results) {}

    record MovieSummary(
            long id,
            String title,
            String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("poster_path") String posterPath) {}

    record MovieDetails(
            long id,
            String title,
            @JsonProperty("original_title") String originalTitle,
            @JsonProperty("release_date") String releaseDate,
            String overview,
            @JsonProperty("poster_path") String posterPath,
            double popularity,
            int runtime,
            List<Genre> genres,
            @JsonProperty("spoken_languages") List<SpokenLanguage> spokenLanguages,
            @JsonProperty("production_countries") List<ProductionCountry> productionCountries,
            Credits credits) {}

    record Genre(String name) {}

    record SpokenLanguage(@JsonProperty("english_name") String englishName) {}

    record ProductionCountry(String name) {}

    record Credits(List<CastMember> cast, List<CrewMember> crew) {}

    record CastMember(String name) {}

    record CrewMember(String name, String job) {}
}