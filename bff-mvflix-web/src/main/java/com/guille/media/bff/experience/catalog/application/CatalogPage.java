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
     * Capabilities derivadas de datos reales, ramificadas por tipo de fila:
     *
     * <ul>
     *   <li><b>ASSET</b>:
     *       <ul>
     *         <li>UNIDENTIFIED (present): identificar — el único con endpoint
     *             real ({@code POST /web/libraries/assets/{id}/identify}).</li>
     *         <li>MISSING (archivo desaparecido): ninguna acción.</li>
     *         <li>{@code viewDetail} y {@code delete} se ocultan siempre: no
     *             existe vista ni borrado de asset, y usar el asset ID como
     *             movie ID borraría un recurso ajeno con el mismo número.</li>
     *       </ul>
     *   </li>
     *   <li><b>MEDIA</b>:
     *       <ul>
     *         <li>play: READY + origen válido + archivo presente (LOCAL).</li>
     *         <li>viewDetail/editMetadata/changeVisibility/manageSharing:
     *             scope OWNED ⇒ el dueño gestiona su ficha.</li>
     *         <li>linkProvider/unlinkProvider: solo MOVIE; exactamente una
     *             según providerStatus. VIDEO no se vincula.</li>
     *         <li>delete: bloqueado para INVALID (conciliar orígenes) y
     *             MISSING (reconciliar el archivo).</li>
     *         <li>identify: no aplica (ya es una media identificada).</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p>FAILED/retry NO se modela aquí: un fallo de subida compensado
     * pertenece a Add Media/Activity, no al catálogo, y aún no existe estado
     * durable ni dueño del mismo.
     */
    public Capabilities getCapabilities() {
      if ("ASSET".equals(this.key.type())) {
        return assetCapabilities();
      }
      return mediaCapabilities();
    }

    private Capabilities assetCapabilities() {
      // Solo un asset PRESENTE (UNIDENTIFIED) puede identificarse; un MISSING
      // no tiene archivo en disco, así que identify queda false.
      boolean identifiable = Boolean.TRUE.equals(this.assetPresent);
      return new Capabilities(
          false, // play
          false, // viewDetail: no hay GET de asset; /web/media/{id} es de movie
          false, // editMetadata
          false, // changeVisibility
          false, // manageSharing
          false, // linkProvider
          false, // unlinkProvider
          identifiable, // identify
          false); // delete: no existe; assetId≠movieId, borraría otro recurso
    }

    private Capabilities mediaCapabilities() {
      boolean invalid = "INVALID".equals(this.source);
      boolean missing = "MISSING".equals(this.displayStatus);
      boolean movie = "MOVIE".equals(this.kind);
      boolean linked = "LINKED".equals(this.providerStatus);
      return new Capabilities(
          playable(),
          true,
          true,
          true,
          true,
          movie && !linked,
          movie && linked,
          false,
          !invalid && !missing);
    }

    public record Capabilities(
        boolean play,
        boolean viewDetail,
        boolean editMetadata,
        boolean changeVisibility,
        boolean manageSharing,
        boolean linkProvider,
        boolean unlinkProvider,
        boolean identify,
        boolean delete) {}
  }

  public record Key(String type, Long id) {}
}
