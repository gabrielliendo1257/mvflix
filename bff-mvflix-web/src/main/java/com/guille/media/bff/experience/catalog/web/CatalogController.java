package com.guille.media.bff.experience.catalog.web;

import com.guille.media.bff.experience.catalog.application.ChangeCatalogVisibility;
import com.guille.media.bff.experience.catalog.application.GetCatalog;
import com.guille.media.bff.experience.catalog.application.port.CatalogActions.ActionRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Experiencia Catalog: administración del contenido propio. La autorización
 * real vive en movies; el controller solo compone vistas y enruta acciones
 * tipadas hacia application (nunca hacia servicios legacy directamente).
 */
@Tag(name = "Catalog", description = "Grilla de administración del contenido propio")
@RestController
@RequestMapping(value = "/web/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
public class CatalogController {

  private final GetCatalog getCatalog;
  private final ChangeCatalogVisibility changeCatalogVisibility;

  public CatalogController(GetCatalog getCatalog, ChangeCatalogVisibility changeCatalogVisibility) {
    this.getCatalog = getCatalog;
    this.changeCatalogVisibility = changeCatalogVisibility;
  }

  @Operation(summary = "Mis películas paginadas (owned): summary + items + metadatos de página")
  @GetMapping
  public Mono<CatalogPageResponse> catalog(
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String sort,
      @RequestParam(required = false) String dir) {
    return this.getCatalog.execute(page, size, q, status, sort, dir)
        .map(CatalogPageResponse::from);
  }

  /** Acción tipada sobre la selección; el progreso llega por SSE de activity. */
  @Operation(summary = "Cambia visibilidad de la selección (202 con job; progreso por SSE)")
  @PostMapping(value = "/actions/change-visibility",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<CatalogActionResponse>> changeVisibility(
      @RequestBody ChangeVisibilityAction action) {
    return this.changeCatalogVisibility
        .execute(new ActionRequest(
            action.movieIds(), action.libraryIds(),
            action.visibility(), action.sharedWith()))
        .map(job -> ResponseEntity.accepted().body(CatalogActionResponse.from(job)));
  }
}
