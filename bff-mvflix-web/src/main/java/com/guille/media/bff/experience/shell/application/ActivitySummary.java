package com.guille.media.bff.experience.shell.application;

import com.guille.media.bff.app.service.JobStore;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/** Puerto interno del contexto hacia los jobs propios del usuario. */
@Component
@RequiredArgsConstructor
public class ActivitySummary {

  private final JobStore jobStore;

  public Mono<ShellActivity> summaryFor(String ownerSubject) {
    return this.jobStore.counts(ownerSubject)
        .map(counts -> new ShellActivity(counts.running(), counts.failed()));
  }
}
