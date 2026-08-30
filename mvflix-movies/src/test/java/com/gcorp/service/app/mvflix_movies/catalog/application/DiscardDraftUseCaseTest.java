package com.gcorp.service.app.mvflix_movies.catalog.application;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.shared.application.security.AuthenticatedUser;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DiscardDraftUseCaseTest {
  private static final CatalogItemId ID = CatalogItemId.of(7L);
  private static final UUID CORRELATION_ID = UUID.randomUUID();

  @Mock CatalogItemRepository repository;
  @Mock UserProvider userProvider;
  @Mock CatalogItemDeletionTransaction deletionTransaction;

  @Test
  void deletesOwnedDraftWithActorAndCorrelation() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(user("pepe")));
    when(repository.findById(ID)).thenReturn(Mono.just(item("pepe", CatalogItemStatus.DRAFT)));
    when(deletionTransaction.deleteImmediately(ID, "pepe", CORRELATION_ID))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            new DiscardDraftUseCase(repository, userProvider, deletionTransaction)
                .execute(ID, CORRELATION_ID))
        .verifyComplete();

    verify(deletionTransaction).deleteImmediately(ID, "pepe", CORRELATION_ID);
  }

  @Test
  void missingDraftIsIdempotent() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(user("pepe")));
    when(repository.findById(ID)).thenReturn(Mono.empty());

    StepVerifier.create(
            new DiscardDraftUseCase(repository, userProvider, deletionTransaction)
                .execute(ID, CORRELATION_ID))
        .verifyComplete();

    verify(deletionTransaction, never()).deleteImmediately(ID, "pepe", CORRELATION_ID);
  }

  @Test
  void readyItemIsNeverDeleted() {
    when(userProvider.getAuthenticatedUser()).thenReturn(Mono.just(user("pepe")));
    when(repository.findById(ID)).thenReturn(Mono.just(item("pepe", CatalogItemStatus.READY)));

    StepVerifier.create(
            new DiscardDraftUseCase(repository, userProvider, deletionTransaction)
                .execute(ID, CORRELATION_ID))
        .expectError()
        .verify();

    verify(deletionTransaction, never()).deleteImmediately(ID, "pepe", CORRELATION_ID);
  }

  private static AuthenticatedUser user(String subject) {
    return new AuthenticatedUser(subject, subject + "@example.test");
  }

  private static CatalogItem item(String owner, CatalogItemStatus status) {
    return new CatalogItem(
        ID,
        owner,
        "Draft",
        status,
        EnrichmentStatus.RAW,
        1L,
        null,
        Visibility.PRIVATE,
        Set.of(),
        CatalogItemKind.MOVIE);
  }
}
