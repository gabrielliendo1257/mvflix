package com.guille.media.bff.presenter.api;

import com.guille.media.bff.app.service.Job;
import com.guille.media.bff.app.service.JobStore;

import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Activity: estado y progreso de los trabajos (scan, identify, enrich, bulk...).
 * El listado devuelve los más recientes; el SSE empuja el progreso en vivo.
 */
@RestController
@RequestMapping("/web/activity")
public class WebActivityController {

    private final JobStore jobStore;

    public WebActivityController(JobStore jobStore) {
        this.jobStore = jobStore;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Job> recent(@RequestParam(defaultValue = "20") int limit) {
        return this.jobStore.recent(limit);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Job>> events(@PathVariable String id) {
        return this.jobStore
                .events(id)
                .map(job -> ServerSentEvent.builder(job).event("progress").build());
    }
}