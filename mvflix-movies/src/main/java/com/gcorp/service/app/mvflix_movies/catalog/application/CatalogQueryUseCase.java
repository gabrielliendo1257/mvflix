package com.gcorp.service.app.mvflix_movies.catalog.application;

import com.gcorp.service.app.mvflix_movies.shared.application.security.UserProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Read model de administración del catálogo propio. NO es un agregado ni un
 * domain service: compone movies + media + media_assets + movie_shares en una
 * consulta controlada para la grilla del dueño.
 *
 * <p>Normalización de la consulta: tamaño 1..50 (default 25), orden con
 * whitelist y dirección explícita — el DEFAULT es DESC (recientemente
 * agregado primero, coherente con el sort default updated_at).
 */
@Service
@RequiredArgsConstructor
public class CatalogQueryUseCase {

  static final int DEFAULT_SIZE = 25;
  static final int MAX_SIZE = 50;

  /**
   * Vocabulario operacional aceptado por el filtro; coincide con
   * display_status de la proyección. DRAFT se acepta como alias legacy de
   * PROCESSING; cualquier otro valor se ignora (sin filtro) dejando warn.
   */
  /**
   * Vocabulario operacional aceptado por el filtro; coincide con
   * display_status de la proyección. DRAFT se acepta como alias legacy de
   * PROCESSING. INVALID NO es filtro: pertenece a source (la fila con doble
   * origen se filtra como ATTENTION).
   */
  private static final java.util.Set<String> FILTERABLE =
      java.util.Set.of("READY", "PROCESSING", "MISSING", "ATTENTION", "DRAFT");

  private final CatalogViewRepository viewRepository;
  private final UserProvider userProvider;

  public Mono<CatalogPageView> execute(
      Integer page, Integer size, String search, String status, String sort, String direction) {
    int safeSize = size == null || size < 1
        ? DEFAULT_SIZE
        : Math.min(size, MAX_SIZE);
    int safePage = page == null || page < 0 ? 0 : page;
    String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
    CatalogReadQuery.SortField sortField = CatalogReadQuery.SortField.from(sort);
    // Sin dir => DESC (recientemente agregado primero). Solo "asc" sube.
    boolean ascending = direction != null && direction.equalsIgnoreCase("asc");

    return this.userProvider
        .getAuthenticatedUser()
        .flatMap(user -> this.viewRepository.page(new CatalogReadQuery(
            user.subject(), safePage, safeSize,
            normalizedSearch,
            normalizeStatus(status),
            sortField, ascending)));
  }

  static String normalizeStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    String upper = status.trim().toUpperCase();
    if (!FILTERABLE.contains(upper)) {
      return null;
    }
    return "DRAFT".equals(upper) ? "PROCESSING" : upper;
  }
}
