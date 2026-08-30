package com.gcorp.service.app.mvflix_movies.catalog.domain.item;

/**
 * Ciclo de vida del catálogo.
 *
 * <ul>
 *   <li>{@code DRAFT}: creado, sin contenido asociado todavía.</li>
 *   <li>{@code READY}: publicado, reproducible.</li>
 *   <li>{@code DELETING}: borrado durable en curso. Estado terminal operativo:
 *       la media ya no se completa, edita, cambia de acceso ni reproduce. Un
 *       nuevo {@code requestDeletion} es idempotente.</li>
 * </ul>
 */
public enum CatalogItemStatus {
    DRAFT,
    READY,
    DELETING
}
