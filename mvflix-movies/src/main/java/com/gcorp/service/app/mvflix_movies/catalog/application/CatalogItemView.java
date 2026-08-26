package com.gcorp.service.app.mvflix_movies.catalog.application;

import java.util.List;

/**
 * Fila de la proyección de administración del catálogo. Es un READ MODEL:
 * combina movies + media + media_assets + movie_shares sin meter esa
 * composición en el agregado {@code Movie}.
 *
 * <p>Dos claves estables según el tipo de fila:
 * <ul>
 *   <li>{@code MEDIA/id}: película (identificada o DRAFT).</li>
 *   <li>{@code ASSET/id}: archivo de biblioteca aún SIN identificar.</li>
 * </ul>
 * Un asset no identificado no tiene movie, status de dominio ni metadata:
 * {@code mediaId}, {@code status}, {@code kind} y {@code providerStatus} son
 * nulos y {@code displayStatus} es {@code UNIDENTIFIED} con {@code source}
 * {@code LOCAL}.
 *
 * <p>{@code displayStatus} es una proyección operacional derivada, NO un
 * estado del dominio.
 *
 * <p>{@code source} habla de mecanismos de almacenamiento lógicos (MANAGED,
 * LOCAL), nunca de infraestructura concreta (MinIO/S3/filesystem).
 */
public record CatalogItemView(
    Key key,
    Long mediaId,
    Long assetId,
    /** Presencia en disco del asset elegido para reproducción; null si no aplica. */
    Boolean assetPresent,
    String title,
    String posterUrl,
    Integer year,
    String duration,
    String kind,
    String status,
    String displayStatus,
    String source,
    String visibility,
    int sharedWithCount,
    String providerStatus) {

  public record Key(String type, Long id) {
    public static Key media(long id) {
      return new Key("MEDIA", id);
    }

    public static Key asset(long id) {
      return new Key("ASSET", id);
    }
  }

  public enum Source {
    MANAGED, LOCAL, NONE,
    /** Doble origen declarado (media + asset identificado): no se elige en silencio. */
    INVALID
  }

  public enum ProviderStatus {
    LINKED, NONE;

    public static ProviderStatus fromTmdbId(Long tmdbId) {
      return tmdbId == null ? NONE : LINKED;
    }

    public static final List<String> NAMES =
        List.of(LINKED.name(), NONE.name());
  }
}
