package com.guille.media.bff.experience.catalog.application;

/**
 * Parámetros de la grilla de administración. Sin scope especulativo: el
 * catálogo de gestión es OWNED por construcción; Home/Search global usan la
 * lectura VISIBLE por otros caminos.
 */
public record CatalogQuery(int limit) {

  public static final int DEFAULT_LIMIT = 20;
  public static final int MAX_LIMIT = 50;

  public static CatalogQuery withLimit(Integer limit) {
    int effective = limit == null || limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
    return new CatalogQuery(effective);
  }
}
