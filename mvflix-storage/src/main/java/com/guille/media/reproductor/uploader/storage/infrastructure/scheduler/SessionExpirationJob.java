package com.guille.media.reproductor.uploader.storage.infrastructure.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.domain.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Expira las sesiones de upload ({@code PENDING}) que superan su TTL y libera
 * la cuota que habían reservado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionExpirationJob {

  private final StorageService storageService;

  @Value("${storage.session.time-to-live:60m}")
  private Duration timeToLive;

  @Scheduled(fixedDelayString = "${storage.session.expiration-check-ms:60000}")
  public void expireStaleSessions() {
    Instant cutoff = Instant.now().minus(this.timeToLive);

    this.storageService
        .expireStaleSessions(cutoff)
        .subscribe(
            expired ->
                log.info(
                    "Expired {} stale upload sessions (cutoff={}, ttl={})",
                    expired, cutoff, this.timeToLive),
            error -> log.error("Error expiring stale upload sessions", error));
  }
}
