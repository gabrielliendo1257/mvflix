package com.guille.media.bff.experience.catalog.application;

import com.guille.media.bff.experience.catalog.application.port.CatalogProjection;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * "Administrar mi contenido": grilla del dueño sobre la proyección owned de
 * movies (paginación, búsqueda, filtros y orden resueltos en SQL por el dueño
 * del catálogo). Nunca mezcla contenido ajeno con acciones de administración;
 * Home/Search global siguen en la lectura VISIBLE.
 */
@Service
@RequiredArgsConstructor
public class GetCatalog {

  static final int DEFAULT_SIZE = 25;
  static final int MAX_SIZE = 50;

  private final CatalogProjection projection;

  public Mono<CatalogPage> execute(
      Integer page, Integer size, String search, String status, String sort, String direction) {
    int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    int safePage = page == null || page < 0 ? 0 : page;
    return this.projection.page(
        safePage,
        safeSize,
        blankToNull(search),
        blankToNull(status),
        blankToNull(sort),
        blankToNull(direction));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
