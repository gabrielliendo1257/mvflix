package com.guille.media.bff.experience.catalog.application;

import com.guille.media.bff.experience.catalog.application.port.CatalogActions;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Acción tipada de la experiencia: cambia visibilidad de la selección.
 * La política de validación y el trabajo en background son detalle del
 * orquestador al que se delega vía puerto; aquí solo intención.
 */
@Service
@RequiredArgsConstructor
public class ChangeCatalogVisibility {

  private final CatalogActions actions;

  public Mono<CatalogActions.CatalogActionJob> execute(CatalogActions.ActionRequest request) {
    return this.actions.changeVisibility(request);
  }
}
