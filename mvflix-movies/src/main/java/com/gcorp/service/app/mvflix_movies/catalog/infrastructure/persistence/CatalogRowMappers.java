package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.gcorp.service.app.mvflix_movies.catalog.application.CatalogItemView;

import io.r2dbc.spi.Row;

/** Traducción de filas de la proyección al read model de aplicación. */
final class CatalogRowMappers {

    private CatalogRowMappers() {
    }

    static CatalogItemView toView(Row row) {
        Long mediaId = row.get("id", Long.class);
        Long assetId = row.get("asset_id", Long.class);
        String yearText = row.get("year_text", String.class);
        Integer year = yearText == null ? null : safeInt(yearText);
        return new CatalogItemView(
                CatalogItemView.Key.media(mediaId),
                mediaId,
                assetId,
                row.get("asset_present", Boolean.class),
                row.get("title", String.class),
                row.get("poster_url", String.class),
                year,
                row.get("duration", String.class),
                row.get("kind", String.class),
                row.get("status", String.class),
                row.get("display_status", String.class),
                row.get("source", String.class),
                row.get("visibility", String.class),
                (int) (long) row.get("shared_count", Long.class),
                row.get("provider_status", String.class));
    }

    private static Integer safeInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException error) {
            return null;
        }
    }
}
