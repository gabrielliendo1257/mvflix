package com.guille.media.bff.app.service;

import com.guille.media.bff.app.dto.BulkVisibilityJobDto;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado en memoria de los trabajos de visibilidad en lote (una sola instancia
 * del BFF en dev). El sink con replay-latest permite a un front que se conecte
 * tarde recibir el último progreso y el cierre del stream.
 */
@Slf4j
@Component
public class BulkVisibilityJobStore {

    private final Map<String, Sinks.Many<BulkVisibilityJobDto>> jobs = new ConcurrentHashMap<>();

    public void create(String jobId) {
        Sinks.Many<BulkVisibilityJobDto> sink =
                Sinks.many().replay().latest();
        this.jobs.put(jobId, sink);
    }

    public Flux<BulkVisibilityJobDto> events(String jobId) {
        Sinks.Many<BulkVisibilityJobDto> sink = this.jobs.get(jobId);
        return sink == null ? Flux.empty() : sink.asFlux();
    }

    public void emit(String jobId, BulkVisibilityJobDto progress) {
        Sinks.Many<BulkVisibilityJobDto> sink = this.jobs.get(jobId);
        if (sink != null) {
            sink.tryEmitNext(progress);
        }
    }

    public void complete(String jobId, BulkVisibilityJobDto finalState) {
        Sinks.Many<BulkVisibilityJobDto> sink = this.jobs.get(jobId);
        if (sink != null) {
            sink.tryEmitNext(finalState);
            sink.tryEmitComplete();
        }
    }
}