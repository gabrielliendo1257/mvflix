package com.gcorp.service.app.mvflix_movies.infrastructure.scheduler;

import com.gcorp.service.app.mvflix_movies.domain.service.MovieCleanupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Purga películas DRAFT que superan su TTL y nunca fueron completadas ni eliminadas.
 *
 * <p>Es seguro purgar pasado el TTL de sesiones de upload del storage (60m por defecto):
 * a esa edad el objeto y la cuota ya fueron liberados por la limpieza del storage, así que
 * aquí solo se elimina metadata.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftCleanupJob {

    private final MovieCleanupService movieCleanupService;

    @Value("${movies.draft.time-to-live:24h}")
    private Duration timeToLive;

    @Scheduled(fixedDelayString = "${movies.draft.cleanup-check-ms:3600000}")
    public void purgeStaleDrafts() {
        Instant cutoff = Instant.now().minus(this.timeToLive);

        this.movieCleanupService
                .purgeDrafts(cutoff)
                .subscribe(
                        purged ->
                                log.debug(
                                        "Draft cleanup done: purged={}, cutoff={}, ttl={}",
                                        purged,
                                        cutoff,
                                        this.timeToLive),
                        error -> log.error("Error purging stale drafts", error));
    }
}