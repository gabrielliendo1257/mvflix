package com.guille.media.bff.experience.catalog.web;

import java.util.List;

/**
 * Acción tipada de cambio de visibilidad sobre la selección del catálogo
 * (movieIds directos y/o bibliotecas enteras). Contrato explícito en vez de
 * un "action" genérico: valida mejor y genera mejor OpenAPI.
 */
public record ChangeVisibilityAction(
    List<Long> movieIds,
    List<Long> libraryIds,
    String visibility,
    List<String> sharedWith) {}
