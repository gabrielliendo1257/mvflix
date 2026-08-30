package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemAccessDeniedException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemConflictException;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DiscardDraftUseCase {
  private final CatalogItemRepository repository;
  private final UserProvider userProvider;
  private final CatalogItemDeletionTransaction deletionTransaction;

  public Mono<Void> execute(CatalogItemId id) {
    return execute(id, UUID.randomUUID());
  }

  public Mono<Void> execute(CatalogItemId id, UUID correlationId) {
    return userProvider
        .getAuthenticatedUser()
        .flatMap(
            user ->
                repository
                    .findById(id)
                    .flatMap(
                        item -> {
                          if (!item.isOwnedBy(user.subject())) {
                            return Mono.error(
                                new CatalogItemAccessDeniedException(
                                    "Movie not owned by actor: " + id.value()));
                          }
                          if (item.getStatus() != CatalogItemStatus.DRAFT) {
                            return Mono.error(
                                new CatalogItemConflictException(
                                    "Only DRAFT movies can be discarded: " + id.value()));
                          }
                          return deletionTransaction.deleteImmediately(
                              id, user.subject(), correlationId);
                        })
                    .switchIfEmpty(Mono.empty()));
  }
}
