package com.guille.media.bff.experience.catalog.infrastructure.http;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.guille.media.bff.experience.catalog.application.CatalogPage;
import com.guille.media.bff.experience.catalog.application.port.CatalogProjection;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Adapter hacia mvflix-movies:/api/v1/catalog. Aquí viven las anotaciones
 * Jackson del downstream y la tolerancia a campos desconocidos; application
 * recibe un modelo puro sin dependencias de wire.
 */
@Component
public class CatalogProjectionAdapter implements CatalogProjection {

  private final WebClient moviesWebClient;

  public CatalogProjectionAdapter(@Qualifier("moviesWebClient") WebClient moviesWebClient) {
    this.moviesWebClient = moviesWebClient;
  }

  @Override
  public Mono<CatalogPage> page(
      int page, int size, String search, String status, String sort, String direction) {
    return this.moviesWebClient
        .get()
        .uri(uriBuilder -> catalogUri(uriBuilder,
                page, size, search, status, sort, direction).build())
        .retrieve()
        .bodyToMono(DownstreamPage.class)
        .map(CatalogProjectionAdapter::toApplication);
  }

  /** Package-private: testeable sin servidor HTTP. */
  static <B extends org.springframework.web.util.UriBuilder> B catalogUri(
      B builder, int page, int size, String search, String status, String sort, String direction) {
    builder.path("/api/v1/movies/catalog")
        .queryParam("page", page)
        .queryParam("size", size);
    if (search != null && !search.isBlank()) {
      builder.queryParam("q", search);
    }
    if (status != null && !status.isBlank()) {
      builder.queryParam("status", status);
    }
    if (sort != null && !sort.isBlank()) {
      builder.queryParam("sort", sort);
    }
    if (direction != null && !direction.isBlank()) {
      builder.queryParam("dir", direction);
    }
    return builder;
  }

  static CatalogPage toApplication(DownstreamPage page) {
    var summary = new CatalogPage.Summary(
        page.summary().total(), page.summary().ready(), page.summary().needsAttention());
    List<CatalogPage.Item> items = page.items() == null ? List.of() : page.items().stream()
        .map(i -> new CatalogPage.Item(
            new CatalogPage.Key(i.key().type(), i.key().id()),
            i.mediaId(), i.assetId(), i.assetPresent(),
            i.title(), i.posterUrl(), i.year(), i.duration(), i.kind(),
            i.status(), i.displayStatus(), i.source(), i.visibility(),
            i.sharedWithCount(), i.providerStatus()))
        .toList();
    return new CatalogPage(summary, items, page.page(), page.size(), page.total(),
        page.totalPages());
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record DownstreamPage(
      DownstreamSummary summary,
      List<DownstreamItem> items,
      int page,
      int size,
      long total,
      int totalPages) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record DownstreamSummary(long total, long ready, long needsAttention) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record DownstreamItem(
      DownstreamKey key,
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
      String providerStatus) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record DownstreamKey(String type, Long id) {}
}
