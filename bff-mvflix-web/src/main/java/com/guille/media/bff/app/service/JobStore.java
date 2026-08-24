package com.guille.media.bff.app.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de {@link Job}s (una sola instancia del BFF en dev).
 * Cada Job lleva un sink con replay-latest para que un front que se conecte
 * tarde reciba el último progreso y el cierre del stream (SSE).
 */
@Slf4j
@Component
public class JobStore {

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();
    private final Map<String, Sinks.Many<Job>> sinks = new ConcurrentHashMap<>();

    public Job start(String id, String ownerSubject, JobType type) {
        Job job = Job.start(id, ownerSubject, type);
        this.jobs.put(id, job);
        this.sinks.put(id, Sinks.many().replay().latest());
        return job;
    }

    public void update(String id, Job job) {
        this.jobs.put(id, job);
        Sinks.Many<Job> sink = this.sinks.get(id);
        if (sink != null) {
            sink.tryEmitNext(job);
        }
    }

    public void complete(String id, Job job) {
        this.jobs.put(id, job);
        Sinks.Many<Job> sink = this.sinks.get(id);
        if (sink != null) {
            sink.tryEmitNext(job);
            sink.tryEmitComplete();
        }
    }

    public Flux<Job> events(String id) {
        Sinks.Many<Job> sink = this.sinks.get(id);
        return sink == null ? Flux.empty() : sink.asFlux();
    }

    public Mono<Job> find(String id) {
        return Mono.justOrEmpty(this.jobs.get(id));
    }

    /** Solo los jobs del propietario, más recientes primero. */
    public Flux<Job> recent(String ownerSubject, int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        return Flux.fromIterable(this.jobs.values().stream()
                .filter(job -> job.ownedBy(ownerSubject))
                .sorted(Comparator.comparing(Job::createdAt).reversed())
                .limit(safeLimit)
                .toList());
    }

    /** El job solo si existe Y pertenece al propietario (sin filtrar existencia). */
    public Mono<Job> findOwned(String id, String ownerSubject) {
        return Mono.justOrEmpty(this.jobs.get(id))
                .filter(job -> job.ownedBy(ownerSubject));
    }
}