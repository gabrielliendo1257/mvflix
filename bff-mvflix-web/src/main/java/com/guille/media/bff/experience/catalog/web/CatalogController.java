package com.guille.media.bff.experience.catalog.web;

import com.guille.media.bff.experience.catalog.application.CatalogPage;
import com.guille.media.bff.experience.catalog.application.GetCatalog;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Experiencia Catalog: administración del contenido propio. La autorización
 * real vive en movies (proyección owned bajo el JWT del usuario); aquí solo
 * se compone la vista para la grilla del front.
 */
@Tag(name = "Catalog", description = "Grilla de administración del contenido propio")
@RestController
@RequestMapping(value = "/web/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

  private final GetCatalog getCatalog;

  public CatalogController(GetCatalog getCatalog) {
    this.getCatalog = getCatalog;
  }

  @Operation(summary = "Mis películas paginadas (owned): summary + items + metadatos de página")
  @GetMapping
  public Mono<CatalogPage> catalog(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String dir) {
    return this.getCatalog.execute(page, size, q, status, sort, dir);
  }
}
