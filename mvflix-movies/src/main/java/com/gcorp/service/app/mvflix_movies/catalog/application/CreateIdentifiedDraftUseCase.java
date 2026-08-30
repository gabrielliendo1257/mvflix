package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItem;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemRepository;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;
import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import java.util.UUID;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.CatalogItemAdded;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.CatalogSemanticOutbox;

import java.util.List;

/**
 * Alta guiada (Add Media): crea el DRAFT ya identificado —metadata del
 * preview + tmdbId— y aplica el acceso inicial (visibilidad + compartidos)
 * como UNA SOLA unidad transaccional. El BFF coordina la experiencia;
 * las reglas de visibilidad y de identidad del proveedor las valida Movies.
 */
@Slf4j
@Service
public class CreateIdentifiedDraftUseCase {

    private final CatalogItemRepository movieRepository;
    private final UserProvider userProvider;
    private final CatalogSemanticOutbox outbox;

    @org.springframework.beans.factory.annotation.Autowired
    public CreateIdentifiedDraftUseCase(CatalogItemRepository repository, UserProvider userProvider, CatalogSemanticOutbox outbox) { this.movieRepository = repository; this.userProvider = userProvider; this.outbox = outbox == null ? event -> Mono.empty() : outbox; }
    @Deprecated
    public CreateIdentifiedDraftUseCase(CatalogItemRepository repository, UserProvider userProvider) { this(repository, userProvider, event -> Mono.empty()); }

    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<CatalogItem> execute(CreateIdentifiedDraftCommand command) {
        UUID correlationId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        Visibility visibility = command.visibility() == null
                ? Visibility.PRIVATE
                : command.visibility();
        List<String> cleanShared = clean(command.sharedWith());
        if (visibility == Visibility.SHARED && cleanShared.isEmpty()) {
            return Mono.error(new IllegalArgumentException(
                    "SHARED requiere al menos un username en usernames"));
        }
        return this.userProvider
                .getAuthenticatedUser()
                .flatMap(user -> {
                    CatalogItem draft = buildIdentifiedDraft(user.subject(), command)
                            .withAccess(visibility, java.util.Set.copyOf(cleanShared));
                    return this.movieRepository.saveDraftWithAccess(draft)
                            .flatMap(saved -> this.outbox.append(new CatalogItemAdded(eventId, java.time.Instant.now(), user.subject(), correlationId, saved.getId().value(), saved.getOwnerUsername(), saved.getKind().name(), saved.getStatus().name())).thenReturn(saved));
                })
                .doOnNext(saved -> log.info(
                        "Identified draft creado: id={} tmdb={} visibility={} shared={}",
                        saved.getId().value(), saved.isMovie() ? saved.getMovieMetadata().tmdbId() : null,
                        saved.getVisibility(), saved.getSharedWith().size()));
    }

    /**
     * Identidad del proveedor: para MOVIE el tmdbId es obligatorio (el flujo
     * guiado nace de un candidato); VIDEO no se identifica con TMDB.
     */
    private CatalogItem buildIdentifiedDraft(String owner, CreateIdentifiedDraftCommand command) {
        CatalogItemKind kind = command.kind() == null ? CatalogItemKind.MOVIE : command.kind();
        CatalogItem draft = CatalogItem.createDraft(owner, command.metadata(), kind);
        if (kind == CatalogItemKind.MOVIE) {
            if (command.metadata().tmdbId() == null) {
                throw new IllegalArgumentException(
                        "tmdbId es obligatorio para crear un draft identificado de película");
            }
            return draft.linkProviderMetadata(command.metadata());
        }
        return draft;
    }

    private static List<String> clean(List<String> usernames) {
        return usernames == null
                ? List.of()
                : usernames.stream()
                        .filter(u -> u != null && !u.isBlank())
                        .distinct()
                        .toList();
    }
}
