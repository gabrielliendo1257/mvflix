package com.guille.media.bff.experience.catalog.application;

import java.util.List;

/**
 * Modelo de aplicación de la grilla de administración: espejo del downstream
 * SIN anotaciones de wire (esas viven en el adapter) y con las capabilities
 * derivadas aquí, una sola vez, para que el front no reinterpretar estados.
 */
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

  public record Summary(long total, long ready, long needsAttention) {}

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
     * Capability honesta: un LOCAL cuyo archivo desapareció NO es reproducible
     * con DIRECT aunque la película siga READY en el dominio.
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
     * Capabilities derivadas de datos reales; cada campo documenta su origen:
     *
     * <ul>
     *   <li>play: READY + origen válido + archivo presente (LOCAL).</li>
     *   <li>viewDetail/editMetadata/changeVisibility/manageSharing: scope
     *       OWNED ⇒ el dueño gestiona su ficha.</li>
     *   <li>delete: bloqueado para INVALID (conciliar orígenes) y MISSING
     *       (reconciliar el archivo); bulk requiere cleanup durable.</li>
     *   <li>identify: false hasta exponer assets UNIDENTIFIED en la página.</li>
     * </ul>
     */
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

    public record Capabilities(
        boolean play,
        boolean viewDetail,
        boolean editMetadata,
        boolean changeVisibility,
        boolean manageSharing,
        boolean identify,
        boolean delete) {}
  }

  public record Key(String type, Long id) {}
}
