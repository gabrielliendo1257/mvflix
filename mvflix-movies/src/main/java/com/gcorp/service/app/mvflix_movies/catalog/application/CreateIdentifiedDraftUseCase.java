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
import com.gcorp.service.app.mvflix_movies.catalog.application.port.IdentifiedDraftIdempotencyStore;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Alta guiada (Add Media): crea el DRAFT ya identificado —metadata del
 * preview + tmdbId— y aplica el acceso inicial (visibilidad + compartidos)
 * como UNA SOLA unidad transaccional. El BFF coordina la experiencia;
 * las reglas de visibilidad y de identidad del proveedor las valida Movies.
 */
@Slf4j
@Service
public class CreateIdentifiedDraftUseCase {

    private static final String OPERATION = "create-catalog-draft";

    private final CatalogItemRepository movieRepository;
    private final IdentifiedDraftIdempotencyStore idempotencyStore;
    private final UserProvider userProvider;
    private final CatalogSemanticOutbox outbox;

    @org.springframework.beans.factory.annotation.Autowired
    public CreateIdentifiedDraftUseCase(CatalogItemRepository repository, UserProvider userProvider,
            CatalogSemanticOutbox outbox, IdentifiedDraftIdempotencyStore idempotencyStore) { this.movieRepository = repository; this.userProvider = userProvider; this.outbox = outbox == null ? event -> Mono.empty() : outbox; this.idempotencyStore = idempotencyStore; }
    @Deprecated
    public CreateIdentifiedDraftUseCase(CatalogItemRepository repository, UserProvider userProvider) { this(repository, userProvider, event -> Mono.empty(), null); }

    @org.springframework.transaction.annotation.Transactional("connectionFactoryTransactionManager")
    public Mono<CatalogItem> execute(CreateIdentifiedDraftCommand command) {
        UUID correlationId = command.correlationId() == null
                ? UUID.randomUUID()
                : command.correlationId();
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
                    String key = command.idempotencyKey();
                    Visibility effectiveVisibility = visibility;
                    String hash = requestHash(command, effectiveVisibility, cleanShared);
                    Mono<CatalogItem> create = Mono.defer(() -> {
                        CatalogItem draft = buildIdentifiedDraft(user.subject(), command)
                                .withAccess(effectiveVisibility, java.util.Set.copyOf(cleanShared));
                        return this.movieRepository.saveDraftWithAccess(draft)
                                .flatMap(saved -> key == null || key.isBlank()
                                        ? Mono.just(saved)
                                         : this.idempotencyStore.bind(
                                            user.subject(), OPERATION, key, saved.getId()).thenReturn(saved))
                                .flatMap(saved -> this.outbox.append(new CatalogItemAdded(eventId,
                                        java.time.Instant.now(), user.subject(), correlationId,
                                        saved.getId().value(), saved.getOwnerUsername(),
                                        saved.getKind().name(), saved.getStatus().name())).thenReturn(saved));
                    });
                    if (key == null || key.isBlank()) {
                        return create;
                    }
                    return this.idempotencyStore.claim(
                            user.subject(), OPERATION, key, hash)
                            .flatMap(claim -> {
                                if (!hash.equals(claim.requestHash())) {
                                    return Mono.error(new IdempotencyKeyReusedException(key));
                                }
                                return claim.movieId() == null
                                        ? create
                                         : this.movieRepository.findById(claim.movieId());
                            });
                })
                .doOnNext(saved -> log.info(
                        "Identified draft creado: id={} tmdb={} visibility={} shared={}",
                        saved.getId().value(), saved.isMovie() ? saved.getMovieMetadata().tmdbId() : null,
                        saved.getVisibility(), saved.getSharedWith().size()));
    }

    private static String requestHash(CreateIdentifiedDraftCommand command,
                                      Visibility visibility, List<String> sharedWith) {
        MovieMetadata metadata = command.metadata();
        StringBuilder canonical = new StringBuilder("identified-draft:v1");
        append(canonical, "kind", command.kind() == null ? CatalogItemKind.MOVIE : command.kind());
        append(canonical, "visibility", visibility);
        append(canonical, "sharedWith", sharedWith.stream().sorted().toList());
        append(canonical, "title", metadata.title());
        append(canonical, "originalTitle", metadata.originalTitle());
        append(canonical, "year", metadata.year());
        append(canonical, "genres", metadata.genres());
        append(canonical, "popularity", metadata.popularity());
        append(canonical, "duration", metadata.duration());
        append(canonical, "director", metadata.director());
        append(canonical, "cast", metadata.cast());
        append(canonical, "overview", metadata.overview());
        append(canonical, "posterPath", metadata.posterPath());
        append(canonical, "releaseDate", metadata.releaseDate());
        append(canonical, "country", metadata.country());
        append(canonical, "language", metadata.language());
        append(canonical, "awards", metadata.awards());
        append(canonical, "tmdbId", metadata.tmdbId());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void append(StringBuilder target, String name, Object value) {
        String text = canonicalValue(value);
        target.append('|').append(name.length()).append(':').append(name)
                .append('=').append(text.length()).append(':').append(text);
    }

    private static String canonicalValue(Object value) {
        if (value == null) return "<null>";
        if (value instanceof Enum<?> enumeration) return enumeration.name();
        if (value instanceof List<?> list) {
            return list.stream().map(CreateIdentifiedDraftUseCase::canonicalValue)
                    .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        }
        String text = value.toString();
        return text.length() + ":" + text;
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
