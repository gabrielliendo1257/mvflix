package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.catalog.application.EnrichCatalogItemUseCase;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Enriquecimiento de fondo del catalogo RAW (solo si hay token TMDB):
 * procesa de a lotes, cada fallo se loguea y no corta el lote.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${tmdb.api-token:}' != ''")
public class EnrichmentJob {

    private static final int BATCH_SIZE = 10;

    private final CatalogItemRepository movieRepository;
    private final EnrichCatalogItemUseCase enrichMovieUseCase;

    @Scheduled(fixedDelayString = "${movies.enrich.check-ms:3600000}")
    public void enrichPending() {
        this.movieRepository
                .findByEnrichmentStatus(EnrichmentStatus.RAW, BATCH_SIZE)
                .flatMap(movie -> this.enrichMovieUseCase
                        .enrich(movie)
                        .doOnError(error -> log.warn(
                                "Enriquecimiento fallido para movie={}: {}",
                                movie.getId().value(), error.getMessage()))
                        .onErrorResume(error -> Mono.empty()))
                .subscribe(
                        enriched -> log.debug("Lote de enriquecimiento procesado: {}", enriched),
                        error -> log.error("Error en el lote de enriquecimiento", error));
    }
}
