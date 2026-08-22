package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftCleanupUseCase {

    private final MovieRepository movieRepository;

    public Mono<Long> purgeDrafts(Instant cutoff) {
        return this.movieRepository
                .deleteDraftsCreatedBefore(cutoff)
                .doOnNext(
                        purged ->
                                log.info(
                                        "Purgadas {} peliculas DRAFT huerfanas (cutoff={})",
                                        purged,
                                        cutoff));
    }
}