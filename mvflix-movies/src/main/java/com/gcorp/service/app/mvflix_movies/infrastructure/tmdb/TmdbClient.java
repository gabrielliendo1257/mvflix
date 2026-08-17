package com.gcorp.service.app.mvflix_movies.infrastructure.tmdb;

import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.MetadataSource;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieDetails;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieSummary;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.SearchResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * Adapter del port {@link MetadataSource} contra la API v3 de TMDB:
 * {@code GET /search/movie} para el match y {@code GET /movie/{id}} con
 * {@code append_to_response=credits} para el detalle completo.
 */
@Slf4j
@Component
public class TmdbClient implements MetadataSource {

    private static final int MAX_CANDIDATES = 10;

    private final WebClient webClient;
    private final TmdbMovieMapper mapper;
    private final TmdbProperties properties;

    public TmdbClient(WebClient.Builder builder, TmdbProperties properties) {
        this.properties = properties;
        this.mapper = new TmdbMovieMapper(properties.imageBaseUrl());
        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                .build();
    }

    @Override
    public Mono<ExternalMovieSearch> search(String title, Integer year) {
        return this.searchResponse(title, year)
                .flatMap(response -> {
                    if (response.results() == null || response.results().isEmpty()) {
                        log.debug("TMDB sin resultados para: {} ({})", title, year);
                        return Mono.empty();
                    }
                    return Mono.justOrEmpty(this.mapper.toSearch(response.results().get(0)));
                });
    }

    @Override
    public Flux<ExternalMovieSearch> searchCandidates(String query, Integer year) {
        return this.searchResponse(query, year)
                .flatMapMany(response -> Flux.fromIterable(
                        response.results() == null ? List.<MovieSummary>of() : response.results()))
                .take(MAX_CANDIDATES)
                .flatMap(summary -> Mono.justOrEmpty(this.mapper.toSearch(summary)));
    }

    private Mono<SearchResponse> searchResponse(String title, Integer year) {
        this.requireToken();
        return this.webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("query", title)
                        .queryParamIfPresent("year", Optional.ofNullable(year))
                        .queryParam("include_adult", "false")
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(SearchResponse.class);
    }

    @Override
    public Mono<ExternalMovieDetail> findById(long tmdbId) {
        this.requireToken();
        return this.webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("append_to_response", "credits")
                        .build(tmdbId))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MovieDetails.class)
                .map(this.mapper::toDetail);
    }

    private void requireToken() {
        if (!this.properties.tokenConfigured()) {
            throw new IllegalStateException(
                    "TMDB_API_TOKEN no configurado: establece tmdb.api-token para enriquecer");
        }
    }
}