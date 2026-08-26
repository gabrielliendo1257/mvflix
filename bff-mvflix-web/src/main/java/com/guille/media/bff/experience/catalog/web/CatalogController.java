package com.guille.media.bff.experience.catalog.web;

import com.guille.media.bff.app.dto.MovieListItemDto;
import com.guille.media.bff.experience.catalog.application.CatalogQuery;
import com.guille.media.bff.experience.catalog.application.GetCatalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Experiencia Catalog: administración del contenido propio. La autorización
 * real vive en movies (scope=owned bajo el JWT del usuario); aquí solo se
 * compone la vista.
 */
@Tag(name = "Catalog", description = "Grilla de administración del contenido propio")
@RestController
@RequestMapping(value = "/web/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

  private final GetCatalog getCatalog;

  public CatalogController(GetCatalog getCatalog) {
    this.getCatalog = getCatalog;
  }

  @Operation(summary = "Mis películas (scope owned); fila → detalle para editar/administrar")
  @GetMapping
  public Flux<MovieListItemDto> catalog(@RequestParam(required = false) Integer limit) {
    return this.getCatalog.execute(CatalogQuery.withLimit(limit));
  }
}
