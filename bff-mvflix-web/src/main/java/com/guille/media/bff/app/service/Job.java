package com.guille.media.bff.app.service;

import java.time.Instant;

/**
 * Trabajo de primera clase del BFF (scan, identify, enrich, bulk...). Es lo que
 * alimenta la sección Activity. Cada Job pertenece al usuario que lo disparó:
 * el listado y el stream SSE están aislados por propietario.
 */
public record Job(
        String id,
        String ownerSubject,
        JobType type,
        JobStatus status,
        int total,
        int done,
        int failed,
        Instant createdAt,
        Instant updatedAt) {

    public static Job start(String id, String ownerSubject, JobType type) {
        return new Job(id, ownerSubject, type, JobStatus.RUNNING, 0, 0, 0,
                Instant.now(), Instant.now());
    }

    public Job progress(int total, int done, int failed) {
        return new Job(this.id, this.ownerSubject, this.type, JobStatus.RUNNING,
                total, done, failed, this.createdAt, Instant.now());
    }

    public Job completed(int total, int done, int failed) {
        return new Job(this.id, this.ownerSubject, this.type, JobStatus.COMPLETED,
                total, done, failed, this.createdAt, Instant.now());
    }

    public Job failed(int total, int done, int failed) {
        return new Job(this.id, this.ownerSubject, this.type, JobStatus.FAILED,
                total, done, failed, this.createdAt, Instant.now());
    }

    public boolean ownedBy(String subject) {
        return this.ownerSubject.equals(subject);
    }
}
