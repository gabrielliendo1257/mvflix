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

    /**
     * Capability honesta para la UI: un LOCAL cuyo archivo desapareció
     * (assetPresent=false) NO es reproducible con DIRECT, aunque la película
     * siga READY. MISSING como estado de pantalla queda para la próxima
     * iteración; mientras tanto el botón Play no se ofrece.
     */
    public boolean playable() {
      if (!"READY".equals(this.status)) {
        return false;
      }
      return switch (this.source == null ? "" : this.source) {
        case "MANAGED" -> true;
        case "LOCAL" -> Boolean.TRUE.equals(this.assetPresent);
        default -> false;
      };
    }

    /**
     * Capabilities derivadas de datos reales del ítem; Angular las lee sin
     * reinterpretar estados. Cada campo documenta su derivación:
     *
     * <ul>
     *   <li>play: READY + origen válido + archivo presente (LOCAL).</li>
     *   <li>viewDetail/editMetadata/changeVisibility/manageSharing: el scope
     *       es OWNED, así que el dueño siempre puede gestionar su ficha.</li>
     *   <li>delete: bloqueado para INVALID (conciliar orígenes primero) y
     *       MISSING (reconciliar el archivo antes de borrar la ficha); la
     *       coordinación durable con storage es prerrequisito del bulk.</li>
     *   <li>identify: false hasta que la página incluya assets
     *       UNIDENTIFIED.</li>
     * </ul>
     */
    @com.fasterxml.jackson.annotation.JsonProperty("capabilities")
    public Capabilities getCapabilities() {
      boolean invalid = "INVALID".equals(this.source);
      boolean missing = "MISSING".equals(this.displayStatus);
      return new Capabilities(
          playable(),
          true,
          true,
          true,
          true,
          false,
          !invalid && !missing);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Capabilities(
        boolean play,
        boolean viewDetail,
        boolean editMetadata,
        boolean changeVisibility,
        boolean manageSharing,
        boolean identify,
        boolean delete) {}
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Key(String type, Long id) {}
}
