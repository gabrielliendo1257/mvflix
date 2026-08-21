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

    public Job start(String id, JobType type) {
        Job job = Job.start(id, type);
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

    public Flux<Job> recent(int limit) {
        int safeLimit = limit <= 0 ? 20 : limit;
        return Flux.fromIterable(this.jobs.values().stream()
                .sorted(Comparator.comparing(Job::createdAt).reversed())
                .limit(safeLimit)
                .toList());
    }
}