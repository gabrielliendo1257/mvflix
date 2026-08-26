package com.guille.media.bff.experience.catalog.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Contrato de la proyección owned de mvflix-movies (GET /api/v1/catalog).
 * Estructura espejo del downstream; el BFF la compone en su respuesta sin
 * filtrar detalles de storage (source ya es MANAGED|LOCAL, no MinIO/S3).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogPage(
    Summary summary,
    List<Item> items,
    int page,
    int size,
    long total,
    int totalPages) {

  public static CatalogPage empty() {
    return new CatalogPage(new Summary(0, 0, 0), List.of(), 0, 0, 0L, 0);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Summary(long total, long ready, long needsAttention) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Item(
      Key key,
      Long mediaId,
      Long assetId,
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

    public boolean playable() {
      return "READY".equals(this.status)
          && ("MANAGED".equals(this.source) || "LOCAL".equals(this.source));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Key(String type, Long id) {}
}
