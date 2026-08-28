package com.guille.media.reproductor.uploader.storage.managedstorage.application;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.exception.StorageException;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.StorageRepository;
import com.guille.media.reproductor.uploader.storage.managedstorage.domain.port.UserStorageRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Colaboración terminal del ciclo de vida: transición CAS de estado +
 * liberación de cuota reservada, atómicas en una única transacción local.
 *
 * <p>Invariante contable: si la liberación falla, la tx revierte el CAS y la
 * fila conserva su estado anterior (PENDING reintentable por el scheduler o el
 * webhook; COMPLETED nunca queda con bytes ya liberados). Los efectos físicos
 * sobre MinIO NO viven aquí: se ejecutan después del commit, en cada flujo.
 */
@Component
public class TerminalUploadTransition {

  private final StorageRepository storageRepository;
  private final UserStorageRepository userStorageRepository;
  private final TransactionalOperator transactionalOperator;

  TerminalUploadTransition(
      StorageRepository storageRepository,
      UserStorageRepository userStorageRepository,
      TransactionalOperator transactionalOperator) {
    this.storageRepository = storageRepository;
    this.userStorageRepository = userStorageRepository;
    this.transactionalOperator = transactionalOperator;
  }

  /**
   * Aplica la transición esperando {@code expectedStatus} como estado previo
   * de la fila y libera la cuota del objeto. Falla con
   * {@code IllegalStateTransitionException} si otro hilo ganó la carrera; en
   * ese caso no toca la cuota.
   */
  public Mono<StoreObject> transitionAndRelease(
      StoreObject object, StorageSessionStatus expectedStatus) {
    return this.transitionAndRelease(object, expectedStatus, ignored -> Mono.empty());
  }

  public Mono<StoreObject> transitionAndRelease(
      StoreObject object,
      StorageSessionStatus expectedStatus,
      Function<StoreObject, Mono<Void>> afterRelease) {
    return this.transactionalOperator.transactional(
        this.storageRepository
            .updateStatus(object, expectedStatus)
            .flatMap(
                updated ->
                    this.userStorageRepository
                        .releaseStorage(updated.getOwnerUsername(), updated.sizeInBytes())
                        // 0 filas = la cuenta del dueño no existe: contabilidad
                        // rota. Fallamos DENTRO de la tx para que el CAS también
                        // revierta; nunca aceptamos un release fantasma.
                        .filter(rows -> rows == 1)
                        .switchIfEmpty(Mono.error(new StorageException(
                            "Quota release affected 0 rows for owner="
                                + updated.getOwnerUsername()
                                + " (account row missing): uploadId="
                                + updated.getStorageId())))
                         .then(afterRelease.apply(updated))
                         .thenReturn(updated)));
  }
}
