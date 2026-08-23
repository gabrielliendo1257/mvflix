package com.guille.media.bff.infrastructure.persistence;

import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;
import com.guille.media.bff.experience.addmedia.model.AddMediaProcess;
import com.guille.media.bff.experience.addmedia.model.ConcurrentProcessUpdateException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

/**
 * Adaptador EN MEMORIA del estado de proceso Add Media. Mismo compromiso que
 * JobStore: válido para dev/single-instance; NO sobrevive reinicios ni escala
 * horizontal. El swap a un store durable (R2DBC/Redis) solo requiere este
 * adapter - el port ya está en su sitio.
 */
@Component
public class InMemoryAddMediaProcessRepository implements AddMediaProcessRepository {

  private final Map<AddMediaId, AddMediaProcess> processes = new ConcurrentHashMap<>();
  private final Map<String, AddMediaId> idempotencyIndex = new ConcurrentHashMap<>();

  @Override
  public Mono<AddMediaProcess> createIfAbsent(String ownerSubject, String idempotencyKey) {
    return Mono.defer(() -> {
      String key = ownerSubject + ":" + idempotencyKey;
      AtomicReference<AddMediaId> resolved = new AtomicReference<>();
      this.idempotencyIndex.compute(key, (k, existingId) -> {
        if (existingId != null) {
          resolved.set(existingId);
          return existingId;
        }
        AddMediaId fresh = AddMediaId.newId();
        this.processes.put(fresh, AddMediaProcess.starting(fresh, ownerSubject));
        resolved.set(fresh);
        return fresh;
      });
      return Mono.justOrEmpty(this.processes.get(resolved.get()));
    });
  }

  @Override
  public Mono<AddMediaProcess> findById(AddMediaId id) {
    return Mono.justOrEmpty(this.processes.get(id));
  }

  @Override
  public Mono<AddMediaProcess> save(AddMediaProcess process) {
    return Mono.defer(() -> {
      AtomicReference<AddMediaProcess> saved = new AtomicReference<>();
      this.processes.compute(process.id(), (id, existing) -> {
        if (existing != null && existing.version() != process.version() - 1) {
          throw new ConcurrentProcessUpdateException(process.id());
        }
        saved.set(process);
        return process;
      });
      return Mono.justOrEmpty(saved.get());
    });
  }
}
