package com.guille.media.bff.experience.catalog.application;

import com.guille.media.bff.app.ports.MoviesWebClient;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * "Administrar mi contenido": grilla del dueño sobre la proyección owned de
 * movies (paginación real, búsqueda, filtros y orden resueltos en SQL por el
 * dueño del catálogo). Nunca mezcla contenido ajeno con acciones de
 * administración; Home/Search global siguen en la lectura VISIBLE.
 */
@Service
@RequiredArgsConstructor
public class GetCatalog {

  private final MoviesWebClient movies;

  public Mono<CatalogPage> execute(
      Integer page, Integer size, String search, String status, String sort, String direction) {
    return this.movies.catalogPage(
        normalizedPage(page), CatalogQuery.withLimit(size).limit(),
        blankToNull(search),
        blankToNull(status),
        blankToNull(sort),
        blankToNull(direction));
  }

  private static int normalizedPage(Integer page) {
    return page == null || page < 0 ? 0 : page;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
