package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.CatalogMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.Default;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.EnrichmentStatus;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieIdentification;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.VideoMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Sharing;
import com.gcorp.service.app.mvflix_movies.catalog.domain.access.Visibility;

import java.util.Set;


public class CatalogItem {

    private final CatalogItemId id;
    private final OwnerId ownerId;
    private final String title;
    private final CatalogItemStatus status;
    private final EnrichmentStatus enrichmentStatus;
    private final CatalogMetadata metadata;
    private final Visibility visibility;
    private final Sharing sharing;
    private final CatalogItemKind kind;
    private final MovieIdentification identification;

    @Default
    public CatalogItem(
        CatalogItemId id,
        OwnerId ownerId,
        String title,
        CatalogItemStatus status,
        EnrichmentStatus enrichmentStatus,
        CatalogMetadata metadata,
        Visibility visibility,
        Sharing sharing,
        CatalogItemKind kind) {
        this.id = id;
        if (ownerId == null) {
            throw new IllegalArgumentException("catalog item owner is required");
        }
        this.ownerId = ownerId;
        this.title = title;
        this.status = status;
        this.enrichmentStatus = enrichmentStatus;
        this.metadata = metadata;
        this.visibility = visibility == null ? Visibility.PRIVATE : visibility;
        Sharing effectiveSharing = sharing == null ? Sharing.empty() : sharing;
        if (this.visibility == Visibility.SHARED && effectiveSharing.isEmpty()) {
            throw new InvalidCatalogItemAccessException("SHARED requires at least one user");
        }
        this.sharing = this.visibility == Visibility.SHARED ? effectiveSharing : Sharing.empty();
        this.kind = kind == null ? CatalogItemKind.MOVIE : kind;
        if (this.metadata != null && !matchesKind(this.metadata, this.kind)) {
            throw new IllegalArgumentException("metadata does not match catalog kind");
        }
        this.identification = metadata instanceof MovieMetadata movie && movie.providerLink() != null
                ? MovieIdentification.of(movie) : null;
    }

    /** Source compatibility for callers that still provide the owner username. */
    public CatalogItem(
        CatalogItemId id,
        String ownerUsername,
        String title,
        CatalogItemStatus status,
        EnrichmentStatus enrichmentStatus,
        CatalogMetadata metadata,
        Visibility visibility,
        Set<String> sharedWith,
        CatalogItemKind kind) {
        this(id, OwnerId.of(ownerUsername), title, status, enrichmentStatus, metadata,
                visibility, Sharing.of(sharedWith), kind);
    }

    /** Source compatibility for callers constructing the pre-separation shape; the locator is ignored. */
    @Deprecated
    public CatalogItem(CatalogItemId id, String ownerUsername, String title, CatalogItemStatus status,
            EnrichmentStatus enrichmentStatus, Long ignoredObjectId, CatalogMetadata metadata,
            Visibility visibility, Set<String> sharedWith, CatalogItemKind kind) {
        this(id, ownerUsername, title, status, enrichmentStatus, metadata, visibility, sharedWith, kind);
    }

    /**
     * Nacimiento de un item en DRAFT (flujo de upload): el dueño aporta la
     * metadata mínima y el objeto se asocia después mediante MediaRepository.
     */
    public static CatalogItem createDraft(String ownerUsername, MovieMetadata metadata, CatalogItemKind kind) {
        return createDraft(ownerUsername, metadataForKind(metadata, kind), kind);
    }

    public static CatalogItem createDraft(String ownerUsername, CatalogMetadata metadata, CatalogItemKind kind) {
        requireOwner(ownerUsername);
        requireTitle(metadata, effectiveKind(kind));
        requireMetadata(metadata, effectiveKind(kind));
        return new CatalogItem(
                null,
                OwnerId.of(ownerUsername),
                metadata.title(),
                CatalogItemStatus.DRAFT,
                 EnrichmentStatus.RAW,
                 metadata,
                Visibility.PRIVATE,
                Sharing.empty(),
                kind);
    }

    /**
     * Nacimiento de un item desde una biblioteca (media server): el archivo ya
     * existe en el filesystem, por eso nace READY sin objeto gestionado.
     */
    public static CatalogItem fromLibraryAsset(String ownerUsername, MovieMetadata metadata, CatalogItemKind kind) {
        return fromLibraryAsset(ownerUsername, metadataForKind(metadata, kind), kind);
    }

    public static CatalogItem fromLibraryAsset(String ownerUsername, CatalogMetadata metadata, CatalogItemKind kind) {
        requireOwner(ownerUsername);
        requireTitle(metadata, effectiveKind(kind));
        requireMetadata(metadata, effectiveKind(kind));
        return new CatalogItem(
                null,
                OwnerId.of(ownerUsername),
                metadata.title(),
                CatalogItemStatus.READY,
                 EnrichmentStatus.RAW,
                 metadata,
                Visibility.PRIVATE,
                Sharing.empty(),
                kind);
    }

    private static void requireOwner(String ownerUsername) {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            throw new IllegalArgumentException("movie owner is required");
        }
    }

    private static void requireMetadata(CatalogMetadata metadata, CatalogItemKind kind) {
        if (metadata == null) {
            throw new IllegalArgumentException(kind == CatalogItemKind.VIDEO
                    ? "video metadata is required" : "movie metadata is required");
        }
        if (metadata.title() == null || metadata.title().isBlank()) {
            throw new IllegalArgumentException(kind == CatalogItemKind.VIDEO
                    ? "video title is required" : "movie title is required");
        }
        if (!matchesKind(metadata, kind)) {
            throw new IllegalArgumentException("metadata does not match catalog kind");
        }
    }

    private static void requireTitle(CatalogMetadata metadata, CatalogItemKind kind) {
        if (metadata == null || metadata.title() == null || metadata.title().isBlank()) {
            throw new IllegalArgumentException(kind == CatalogItemKind.VIDEO
                    ? "movie title is required" : "movie title is required");
        }
    }

    private static boolean matchesKind(CatalogMetadata metadata, CatalogItemKind kind) {
        return (kind == CatalogItemKind.MOVIE && metadata instanceof MovieMetadata)
                || (kind == CatalogItemKind.VIDEO && metadata instanceof VideoMetadata);
    }

    private static CatalogItemKind effectiveKind(CatalogItemKind kind) {
        return kind == null ? CatalogItemKind.MOVIE : kind;
    }

    private static CatalogMetadata metadataForKind(MovieMetadata metadata, CatalogItemKind kind) {
        if (effectiveKind(kind) == CatalogItemKind.VIDEO
                && (metadata == null || metadata.title() == null || metadata.title().isBlank())) {
            throw new IllegalArgumentException("movie title is required");
        }
        return effectiveKind(kind) == CatalogItemKind.VIDEO
                ? new VideoMetadata(metadata == null ? null : metadata.title(),
                        metadata == null ? null : metadata.overview(), null)
                : metadata;
    }

    public CatalogItemId getId() {
        return this.id;
    }

    public String getOwnerUsername() {
        return this.ownerId.value();
    }

    public OwnerId getOwnerId() {
        return this.ownerId;
    }

    public String getTitle() {
        return this.metadata != null && this.metadata.title() != null
                ? this.metadata.title()
                : this.title;
    }

    public CatalogItemStatus getStatus() {
        return this.status;
    }

    public EnrichmentStatus getEnrichmentStatus() {
        return this.enrichmentStatus;
    }

    public CatalogMetadata getMetadata() {
        return this.metadata;
    }

    public MovieMetadata getMovieMetadata() {
        if (!(this.metadata instanceof MovieMetadata movieMetadata)) {
            throw new CatalogItemConflictException("Catalog item does not contain movie metadata");
        }
        return movieMetadata;
    }

    /** Safe nullable accessor for projections that also contain VIDEO items. */
    public MovieMetadata getMovieMetadataOrNull() {
        return this.metadata instanceof MovieMetadata movieMetadata ? movieMetadata : null;
    }

    /** Identidad externa tipada; null mientras la pelicula no esta identificada. */
    public MovieIdentification getIdentification() {
        return this.identification;
    }

    public Visibility getVisibility() {
        return this.visibility;
    }

    public Set<String> getSharedWith() {
        return this.sharing.users();
    }

    public Sharing getSharing() {
        return this.sharing;
    }

    public CatalogItemKind getKind() {
        return this.kind;
    }

    /** {@code true} si el item representa una película (tipo de contenido). */
    public boolean isMovie() {
        return this.kind == CatalogItemKind.MOVIE;
    }

    /** Solo el dueño puede administrar el item (visibilidad, compartidos, borrado). */
    public boolean isOwnedBy(String username) {
        return this.ownerId.value().equals(username);
    }

    /**
     * Política de acceso al catálogo (fuente de verdad del dominio): el dueño siempre ve;
     * PUBLIC lo ve cualquiera; PRIVATE nadie más; SHARED solo los de la lista de compartidos.
     */
    public boolean isVisibleTo(String username) {
        return isOwnedBy(username)
                || this.visibility == Visibility.PUBLIC
                || (this.visibility == Visibility.SHARED && this.sharing.contains(username));
    }

    /** Transición de dominio: cambia la visibilidad del catálogo (solo el dueño). */
    public CatalogItem withVisibility(Visibility visibility) {
        requireNotDeleting("change visibility");
        if (visibility == Visibility.SHARED && this.sharing.isEmpty()) {
            throw new InvalidCatalogItemAccessException("SHARED requires at least one user");
        }
        Sharing effective = visibility == Visibility.SHARED ? this.sharing : Sharing.empty();
        return new CatalogItem(
                this.id,
                this.ownerId,
                this.title,
                this.status,
                this.enrichmentStatus,
                this.metadata,
                visibility,
                effective,
                this.kind);
    }

    /** Transición de dominio: reemplaza la lista de compartidos (solo el dueño). */
    public CatalogItem withSharedWith(Set<String> sharedWith) {
        return withAccess(this.visibility, sharedWith);
    }

    /**
     * Transición de dominio: acceso completo (visibilidad + compartidos) en
     * una sola decisión. La invariante de acceso vive AQUÍ:
     *
     * <ul>
     *   <li>SHARED exige al menos un usuario compartido.</li>
     *   <li>PRIVATE y PUBLIC ignoran y limpian los compartidos.</li>
     * </ul>
     *
     * Existe para que el cambio se persista como unidad y nunca quede una
     * película SHARED sin sus shares, ni PRIVATE/PUBLIC con residuos.
     */
    public CatalogItem withAccess(Visibility visibility, Set<String> sharedWith) {
        requireNotDeleting("change access");
        Sharing shares = Sharing.of(sharedWith);
        if (visibility == Visibility.SHARED && shares.isEmpty()) {
            throw new InvalidCatalogItemAccessException("SHARED requires at least one user");
        }
        // Solo SHARED retiene los compartidos; el resto los limpia.
        Sharing effective = visibility == Visibility.SHARED ? shares : Sharing.empty();
        return new CatalogItem(
                this.id,
                this.ownerId,
                this.title,
                this.status,
                this.enrichmentStatus,
                this.metadata,
                visibility,
                effective,
                this.kind);
    }

    /** Transición de dominio: reemplaza la metadata (edición manual del dueño). */
    public CatalogItem withMetadata(CatalogMetadata metadata) {
        requireNotDeleting("edit");
        requireMetadata(metadata, this.kind);
        return new CatalogItem(
                this.id,
                this.ownerId,
                metadata.title(),
                this.status,
                this.enrichmentStatus,
                metadata,
                this.visibility,
                this.sharing,
                this.kind);
    }

    /**
     * Reclasifica una película como contenido genérico. El vínculo con el
     * proveedor deja de ser válido y el item deja de estar identificado como
     * película.
     */
    public CatalogItem reclassifyAsVideo(MovieMetadata manualMetadata) {
        return reclassifyAsVideo(metadataForKind(manualMetadata, CatalogItemKind.VIDEO));
    }

    public CatalogItem reclassifyAsVideo(CatalogMetadata manualMetadata) {
        requireNotDeleting("reclassify");
        requireMetadata(manualMetadata, CatalogItemKind.VIDEO);
        return new CatalogItem(
                this.id,
                this.ownerId,
                manualMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                manualMetadata,
                this.visibility,
                this.sharing,
                CatalogItemKind.VIDEO);
    }

    /**
     * Reclasifica contenido genérico como película todavía no identificada.
     * Un proveedor solo puede vincularse después mediante
     * {@link #linkProviderMetadata(MovieMetadata)}.
     */
    public CatalogItem reclassifyAsMovie() {
        CatalogMetadata manualMetadata = this.metadata instanceof MovieMetadata
                ? this.metadata
                : new MovieMetadata(this.metadata.title(), null, null, java.util.List.of(), null,
                        null, null, java.util.List.of(),
                        this.metadata instanceof VideoMetadata video ? video.description() : null,
                        null, null, null, null, java.util.List.of(), null);
        return reclassifyAsMovie(manualMetadata);
    }

    public CatalogItem reclassifyAsMovie(CatalogMetadata manualMetadata) {
        requireNotDeleting("reclassify");
        requireMetadata(manualMetadata, CatalogItemKind.MOVIE);
        MovieMetadata unlinkedMetadata = manualMetadata instanceof MovieMetadata movieMetadata
                ? movieMetadata.withoutProvider()
                : new MovieMetadata(manualMetadata.title(), null, null, java.util.List.of(), null,
                        null, null, java.util.List.of(),
                        manualMetadata instanceof VideoMetadata video ? video.description() : null,
                        null, null, null, null, java.util.List.of(), null);
        return new CatalogItem(
                this.id,
                this.ownerId,
                unlinkedMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                unlinkedMetadata,
                this.visibility,
                this.sharing,
                CatalogItemKind.MOVIE);
    }

    public boolean isDraft() {
        return this.status == CatalogItemStatus.DRAFT;
    }

    /** {@code true} si la media está en borrado durable (no reproducible ni editable). */
    public boolean isDeleting() {
        return this.status == CatalogItemStatus.DELETING;
    }

    /**
     * Transición de dominio: inicia el borrado durable de la media. Idempotente:
     * si ya estaba DELETING devuelve {@code this}; en cualquier otro estado pasa
     * a DELETING. A partir de aquí ninguna mutación es válida.
     */
    public CatalogItem requestDeletion() {
        if (this.status == CatalogItemStatus.DELETING) {
            return this;
        }
        return new CatalogItem(
                this.id,
                this.ownerId,
                this.title,
                CatalogItemStatus.DELETING,
                this.enrichmentStatus,
                this.metadata,
                this.visibility,
                this.sharing,
                this.kind);
    }

    /** Una media DELETING es un estado terminal operativo: rechaza toda mutación. */
    private void requireNotDeleting(String transition) {
        if (this.status == CatalogItemStatus.DELETING) {
            throw new CatalogItemConflictException(
                    "Cannot " + transition + " a movie in DELETING state");
        }
    }

    public boolean isEnriched() {
        return this.enrichmentStatus == EnrichmentStatus.ENRICHED;
    }

    /**
     * Vincula metadata confirmada de un proveedor externo. Solo los items que
     * representan películas pueden tener ese vínculo y la metadata debe incluir
     * el identificador estable del proveedor.
     */
    public CatalogItem linkProviderMetadata(MovieMetadata providerMetadata) {
        requireNotDeleting("link provider");
        if (!isMovie()) {
            throw new CatalogItemConflictException("Only movie items can link provider metadata");
        }
        if (providerMetadata == null || providerMetadata.tmdbId() == null) {
            throw new IllegalArgumentException("provider metadata id is required");
        }
        MovieIdentification.of(providerMetadata);
        requireMetadata(providerMetadata, CatalogItemKind.MOVIE);
        return new CatalogItem(
                this.id,
                this.ownerId,
                providerMetadata.title(),
                this.status,
                EnrichmentStatus.ENRICHED,
                providerMetadata,
                this.visibility,
                this.sharing,
                this.kind);
    }

    /** Vincula una identidad y su metadata confirmada en una sola transicion. */
    public CatalogItem identify(MovieIdentification identification) {
        requireNotDeleting("identify");
        if (!isMovie()) {
            throw new CatalogItemConflictException("Only movie items can be identified");
        }
        if (identification == null || identification.metadata() == null) {
            throw new IllegalArgumentException("identified movie metadata is required");
        }
        return linkProviderMetadata(identification.metadata());
    }

    /** Desvincula el proveedor y devuelve el item a RAW conservando metadata manual. */
    public CatalogItem unlinkProvider() {
        requireNotDeleting("unlink provider");
        if (!isMovie()) {
            throw new CatalogItemConflictException("Only movie items can unlink provider metadata");
        }
        MovieMetadata unlinkedMetadata = getMovieMetadata().withoutProvider();
        return new CatalogItem(
                this.id,
                this.ownerId,
                unlinkedMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                unlinkedMetadata,
                this.visibility,
                this.sharing,
                this.kind);
    }

    /**
     * Transición de dominio: un item en borrador pasa a lista cuando se le asigna su objeto.
     * El locator no forma parte del agregado; el asset gestionado vive en MediaRepository.
     */
    public CatalogItem complete() {
        requireNotDeleting("complete");
        return new CatalogItem(
                this.id,
                this.ownerId,
                this.title,
                CatalogItemStatus.READY,
                this.enrichmentStatus,
                this.metadata,
                this.visibility,
                this.sharing,
                this.kind);
    }

}
