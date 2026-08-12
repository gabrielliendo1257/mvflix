package com.gcorp.service.app.mvflix_movies.domain.service;

import com.gcorp.service.app.mvflix_movies.domain.ports.MovieRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.Instant;

@Slf4j
@Service
public class MovieCleanupServiceImpl implements MovieCleanupService {

    private final MovieRepository movieRepository;

    public MovieCleanupServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
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