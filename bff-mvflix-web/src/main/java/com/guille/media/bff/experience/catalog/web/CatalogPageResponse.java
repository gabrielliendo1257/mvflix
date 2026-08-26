package com.guille.media.bff.experience.catalog.web;

import com.guille.media.bff.experience.catalog.application.CatalogPage;

import java.util.List;

/**
 * Contrato HTTP de la grilla de administración. Espejo explícito de la
 * proyección downstream: si movies cambia su forma, este mapeo es el único
 * punto que se rompe visiblemente en lugar de filtrar campos de más/menos
 * sin aviso.
 */
public record CatalogPageResponse(
    SummaryResponse summary,
    List<ItemResponse> items,
    int page,
    int size,
    long total,
    int totalPages) {

  public static CatalogPageResponse from(CatalogPage page) {
    return new CatalogPageResponse(
        new SummaryResponse(
            page.summary().total(),
            page.summary().ready(),
            page.summary().needsAttention()),
        page.items().stream().map(ItemResponse::from).toList(),
        page.page(), page.size(), page.total(), page.totalPages());
  }

  public record SummaryResponse(long total, long ready, long needsAttention) {}

  public record ItemResponse(
      KeyResponse key,
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
      String providerStatus,
      CapabilitiesResponse capabilities) {

    public static ItemResponse from(CatalogPage.Item item) {
      var caps = item.getCapabilities();
      return new ItemResponse(
          new KeyResponse(item.key().type(), item.key().id()),
          item.mediaId(), item.assetId(), item.assetPresent(),
          item.title(), item.posterUrl(), item.year(), item.duration(),
          item.kind(), item.status(), item.displayStatus(), item.source(),
          item.visibility(), item.sharedWithCount(), item.providerStatus(),
          new CapabilitiesResponse(
              caps.play(), caps.viewDetail(), caps.editMetadata(),
              caps.changeVisibility(), caps.manageSharing(),
              caps.linkProvider(), caps.unlinkProvider(),
              caps.identify(), caps.delete()));
    }

    public record KeyResponse(String type, Long id) {}

    public record CapabilitiesResponse(
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
}
