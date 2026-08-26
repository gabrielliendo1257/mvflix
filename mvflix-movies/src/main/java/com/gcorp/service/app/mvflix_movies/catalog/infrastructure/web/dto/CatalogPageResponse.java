package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.web.dto;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;
import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogPageView;

import java.util.List;

/** Contrato HTTP de la proyección owned; camelCase estable para el BFF. */
public record CatalogPageResponse(
    SummaryResponse summary,
    List<ItemResponse> items,
    int page,
    int size,
    long total,
    int totalPages) {

    public static CatalogPageResponse from(CatalogPageView view) {
        return new CatalogPageResponse(
            new SummaryResponse(
                    view.summary().total(),
                    view.summary().ready(),
                    view.summary().needsAttention()),
            view.items().stream().map(ItemResponse::from).toList(),
            view.page(), view.size(), view.total(), view.totalPages());
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
        String providerStatus) {

        public static ItemResponse from(CatalogItemView item) {
            return new ItemResponse(
                new KeyResponse(item.key().type(), item.key().id()),
                item.mediaId(), item.assetId(), item.assetPresent(),
                item.title(), item.posterUrl(),
                item.year(), item.duration(), item.kind(), item.status(),
                item.displayStatus(), item.source(), item.visibility(),
                item.sharedWithCount(), item.providerStatus());
        }

        public record KeyResponse(String type, Long id) {}
    }
}
