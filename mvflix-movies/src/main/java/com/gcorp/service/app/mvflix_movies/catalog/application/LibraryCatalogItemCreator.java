package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.library.application.port.CatalogItemCreator;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemId;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import java.util.UUID;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.CatalogItemAdded;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.CatalogSemanticOutbox;

/** Implementación de Catalog para la creación solicitada por Library Ingestion. */
@Service
public class LibraryCatalogItemCreator implements CatalogItemCreator {

    private final CatalogItemRepository movieRepository;
    private final CatalogSemanticOutbox outbox;

    @org.springframework.beans.factory.annotation.Autowired
    public LibraryCatalogItemCreator(CatalogItemRepository repository, CatalogSemanticOutbox outbox) { this.movieRepository = repository; this.outbox = outbox == null ? event -> Mono.empty() : outbox; }
    @Deprecated
    public LibraryCatalogItemCreator(CatalogItemRepository repository) { this(repository, event -> Mono.empty()); }

    @Override
    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<CatalogItemId> createFromLibrary(
            String ownerUsername, String title, CatalogItemKind kind) {
        CatalogItem movie = CatalogItem.fromLibraryAsset(
                ownerUsername, MovieMetadata.onlyTitle(title), kind);
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        return this.movieRepository.save(movie)
                .flatMap(saved -> this.outbox.append(new CatalogItemAdded(eventId, java.time.Instant.now(), "system", correlationId, saved.getId().value(), saved.getOwnerUsername(), saved.getKind().name(), saved.getStatus().name())).thenReturn(saved))
                .map(saved -> CatalogItemId.of(saved.getId().value()));
    }

}
