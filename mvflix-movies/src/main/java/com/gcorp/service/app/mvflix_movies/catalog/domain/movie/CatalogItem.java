package com.gcorp.service.app.mvflix_movies.catalog.domain.movie;

import java.util.Set;

public class CatalogItem {

    private final CatalogItemId id;
    private final String ownerUsername;
    private final String title;
    private final CatalogItemStatus status;
    private final EnrichmentStatus enrichmentStatus;
    private final Long objectId;
    private final CatalogMetadata metadata;
    private final CatalogItemVisibility visibility;
    private final Set<String> sharedWith;
    private final MediaKind kind;

    public CatalogItem(
        CatalogItemId id,
        String ownerUsername,
        String title,
        CatalogItemStatus status,
        EnrichmentStatus enrichmentStatus,
        Long objectId,
        CatalogMetadata metadata,
        CatalogItemVisibility visibility,
        Set<String> sharedWith,
        MediaKind kind) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.title = title;
        this.status = status;
        this.enrichmentStatus = enrichmentStatus;
        this.objectId = objectId;
        this.metadata = metadata;
        this.visibility = visibility;
        this.sharedWith = sharedWith == null ? Set.of() : Set.copyOf(sharedWith);
        this.kind = kind == null ? MediaKind.MOVIE : kind;
        if (this.metadata != null && !matchesKind(this.metadata, this.kind)) {
            throw new IllegalArgumentException("metadata does not match catalog kind");
        }
    }

    /**
     * Nacimiento de un item en DRAFT (flujo de upload): el dueño aporta la
     * metadata mínima y el objeto se asocia después con {@link #complete(Long)}.
     */
    public static CatalogItem createDraft(String ownerUsername, MovieMetadata metadata, MediaKind kind) {
        return createDraft(ownerUsername, metadataForKind(metadata, kind), kind);
    }

    public static CatalogItem createDraft(String ownerUsername, CatalogMetadata metadata, MediaKind kind) {
        requireOwner(ownerUsername);
        requireTitle(metadata, effectiveKind(kind));
        requireMetadata(metadata, effectiveKind(kind));
        return new CatalogItem(
                null,
                ownerUsername,
                metadata.title(),
                CatalogItemStatus.DRAFT,
                EnrichmentStatus.RAW,
                null,
                metadata,
                CatalogItemVisibility.PRIVATE,
                Set.of(),
                kind);
    }

    /**
     * Nacimiento de un item desde una biblioteca (media server): el archivo ya
     * existe en el filesystem, por eso nace READY sin objeto subido (objectId
     * null). Es la otra clase de READY, complementaria a {@link #complete(Long)}.
     */
    public static CatalogItem fromLibraryAsset(String ownerUsername, MovieMetadata metadata, MediaKind kind) {
        return fromLibraryAsset(ownerUsername, metadataForKind(metadata, kind), kind);
    }

    public static CatalogItem fromLibraryAsset(String ownerUsername, CatalogMetadata metadata, MediaKind kind) {
        requireOwner(ownerUsername);
        requireTitle(metadata, effectiveKind(kind));
        requireMetadata(metadata, effectiveKind(kind));
        return new CatalogItem(
                null,
                ownerUsername,
                metadata.title(),
                CatalogItemStatus.READY,
                EnrichmentStatus.RAW,
                null,
                metadata,
                CatalogItemVisibility.PRIVATE,
                Set.of(),
                kind);
    }

    private static void requireOwner(String ownerUsername) {
        if (ownerUsername == null || ownerUsername.isBlank()) {
            throw new IllegalArgumentException("movie owner is required");
        }
    }

    private static void requireMetadata(CatalogMetadata metadata, MediaKind kind) {
        if (metadata == null) {
            throw new IllegalArgumentException(kind == MediaKind.VIDEO
                    ? "video metadata is required" : "movie metadata is required");
        }
        if (metadata.title() == null || metadata.title().isBlank()) {
            throw new IllegalArgumentException(kind == MediaKind.VIDEO
                    ? "video title is required" : "movie title is required");
        }
        if (!matchesKind(metadata, kind)) {
            throw new IllegalArgumentException("metadata does not match catalog kind");
        }
    }

    private static void requireTitle(CatalogMetadata metadata, MediaKind kind) {
        if (metadata == null || metadata.title() == null || metadata.title().isBlank()) {
            throw new IllegalArgumentException(kind == MediaKind.VIDEO
                    ? "movie title is required" : "movie title is required");
        }
    }

    private static boolean matchesKind(CatalogMetadata metadata, MediaKind kind) {
        return (kind == MediaKind.MOVIE && metadata instanceof MovieMetadata)
                || (kind == MediaKind.VIDEO && metadata instanceof VideoMetadata);
    }

    private static MediaKind effectiveKind(MediaKind kind) {
        return kind == null ? MediaKind.MOVIE : kind;
    }

    private static CatalogMetadata metadataForKind(MovieMetadata metadata, MediaKind kind) {
        if (effectiveKind(kind) == MediaKind.VIDEO
                && (metadata == null || metadata.title() == null || metadata.title().isBlank())) {
            throw new IllegalArgumentException("movie title is required");
        }
        return effectiveKind(kind) == MediaKind.VIDEO
                ? new VideoMetadata(metadata == null ? null : metadata.title(),
                        metadata == null ? null : metadata.overview(), null)
                : metadata;
    }

    public CatalogItemId getId() {
        return this.id;
    }

    public String getOwnerUsername() {
        return this.ownerUsername;
    }

    public Long getObjectId() {
        return this.objectId;
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

    public CatalogItemVisibility getVisibility() {
        return this.visibility;
    }

    public Set<String> getSharedWith() {
        return this.sharedWith;
    }

    public MediaKind getKind() {
        return this.kind;
    }

    /** {@code true} si el item representa una película (tipo de contenido). */
    public boolean isMovie() {
        return this.kind == MediaKind.MOVIE;
    }

    /** Solo el dueño puede administrar el item (visibilidad, compartidos, borrado). */
    public boolean isOwnedBy(String username) {
        return this.ownerUsername.equals(username);
    }

    /**
     * Política de acceso al catálogo (fuente de verdad del dominio): el dueño siempre ve;
     * PUBLIC lo ve cualquiera; PRIVATE nadie más; SHARED solo los de la lista de compartidos.
     */
    public boolean isVisibleTo(String username) {
        return isOwnedBy(username)
                || this.visibility == CatalogItemVisibility.PUBLIC
                || (this.visibility == CatalogItemVisibility.SHARED
                        && this.sharedWith.contains(username));
    }

    /** Transición de dominio: cambia la visibilidad del catálogo (solo el dueño). */
    public CatalogItem withVisibility(CatalogItemVisibility visibility) {
        requireNotDeleting("change visibility");
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                this.title,
                this.status,
                this.enrichmentStatus,
                this.objectId,
                this.metadata,
                visibility,
                this.sharedWith,
                this.kind);
    }

    /** Transición de dominio: reemplaza la lista de compartidos (solo el dueño). */
    public CatalogItem withSharedWith(Set<String> sharedWith) {
        requireNotDeleting("change shares");
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                this.title,
                this.status,
                this.enrichmentStatus,
                this.objectId,
                this.metadata,
                this.visibility,
                sharedWith,
                this.kind);
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
    public CatalogItem withAccess(CatalogItemVisibility visibility, Set<String> sharedWith) {
        requireNotDeleting("change access");
        Set<String> shares = sharedWith == null ? Set.of() : Set.copyOf(sharedWith);
        if (visibility == CatalogItemVisibility.SHARED && shares.isEmpty()) {
            throw new InvalidCatalogItemAccessException("SHARED requires at least one user");
        }
        // Solo SHARED retiene los compartidos; el resto los limpia.
        Set<String> effective = visibility == CatalogItemVisibility.SHARED ? shares : Set.of();
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                this.title,
                this.status,
                this.enrichmentStatus,
                this.objectId,
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
                this.ownerUsername,
                metadata.title(),
                this.status,
                this.enrichmentStatus,
                this.objectId,
                metadata,
                this.visibility,
                this.sharedWith,
                this.kind);
    }

    /**
     * Reclasifica una película como contenido genérico. El vínculo con el
     * proveedor deja de ser válido y el item deja de estar identificado como
     * película.
     */
    public CatalogItem reclassifyAsVideo(MovieMetadata manualMetadata) {
        return reclassifyAsVideo(metadataForKind(manualMetadata, MediaKind.VIDEO));
    }

    public CatalogItem reclassifyAsVideo(CatalogMetadata manualMetadata) {
        requireNotDeleting("reclassify");
        requireMetadata(manualMetadata, MediaKind.VIDEO);
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                manualMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                this.objectId,
                manualMetadata,
                this.visibility,
                this.sharedWith,
                MediaKind.VIDEO);
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
        requireMetadata(manualMetadata, MediaKind.MOVIE);
        MovieMetadata unlinkedMetadata = manualMetadata instanceof MovieMetadata movieMetadata
                ? movieMetadata.withoutProvider()
                : new MovieMetadata(manualMetadata.title(), null, null, java.util.List.of(), null,
                        null, null, java.util.List.of(),
                        manualMetadata instanceof VideoMetadata video ? video.description() : null,
                        null, null, null, null, java.util.List.of(), null);
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                unlinkedMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                this.objectId,
                unlinkedMetadata,
                this.visibility,
                this.sharedWith,
                MediaKind.MOVIE);
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
                this.ownerUsername,
                this.title,
                CatalogItemStatus.DELETING,
                this.enrichmentStatus,
                this.objectId,
                this.metadata,
                this.visibility,
                this.sharedWith,
                this.kind);
    }

    /** Una media DELETING es un estado terminal operativo: rechaza toda mutación. */
    private void requireNotDeleting(String transition) {
        if (this.status == CatalogItemStatus.DELETING) {
            throw new CatalogItemConflictException(
                    "Cannot " + transition + " a movie in DELETING state");
        }
    }


    /**
     * READY respaldado por un archivo de biblioteca (sin objeto subido): el
     * playback se resuelve por {@code MediaAsset}, no por el storage del upload.
     */
    public boolean isLibraryBacked() {
        return this.status == CatalogItemStatus.READY && this.objectId == null;
    }

    /** READY con objeto subido al storage (flujo de upload). */
    public boolean isUploaded() {
        return this.objectId != null;
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
        requireMetadata(providerMetadata, MediaKind.MOVIE);
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                providerMetadata.title(),
                this.status,
                EnrichmentStatus.ENRICHED,
                this.objectId,
                providerMetadata,
                this.visibility,
                this.sharedWith,
                this.kind);
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
                this.ownerUsername,
                unlinkedMetadata.title(),
                this.status,
                EnrichmentStatus.RAW,
                this.objectId,
                unlinkedMetadata,
                this.visibility,
                this.sharedWith,
                this.kind);
    }

    /**
     * Transición de dominio: un item en borrador pasa a lista cuando se le asigna su objeto.
     * Solo el {@code objectId} (referencia publica) vive en el agregado; la key del objeto
      * es un secreto interno que queda en {@code ManagedMediaAsset}, nunca en CatalogItem.
     */
    public CatalogItem complete(Long objectId) {
        requireNotDeleting("complete");
        return new CatalogItem(
                this.id,
                this.ownerUsername,
                this.title,
                CatalogItemStatus.READY,
                this.enrichmentStatus,
                objectId,
                this.metadata,
                this.visibility,
                this.sharedWith,
                this.kind);
    }

}
