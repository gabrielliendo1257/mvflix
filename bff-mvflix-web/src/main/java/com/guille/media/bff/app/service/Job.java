package com.guille.media.bff.app.service;

import java.time.Instant;

/**
 * Trabajo de primera clase del BFF (scan, identify, enrich, bulk...). Es lo que
 * alimenta la sección Activity: cada operación potencialmente asíncrona registra
 * un Job con su tipo, estado y progreso (total/done/failed) consultable por el front.
 */
public record Job(
        String id,
        JobType type,
        JobStatus status,
        int total,
        int done,
        int failed,
        Instant createdAt,
        Instant updatedAt) {

    public static Job start(String id, JobType type) {
        return new Job(id, type, JobStatus.RUNNING, 0, 0, 0, Instant.now(), Instant.now());
    }

    public Job progress(int total, int done, int failed) {
        return new Job(this.id, this.type, JobStatus.RUNNING, total, done, failed,
                this.createdAt, Instant.now());
    }

    public Job completed(int total, int done, int failed) {
        return new Job(this.id, this.type, JobStatus.COMPLETED, total, done, failed,
                this.createdAt, Instant.now());
    }

    public Job failed(int total, int done, int failed) {
        return new Job(this.id, this.type, JobStatus.FAILED, total, done, failed,
                this.createdAt, Instant.now());
    }
}