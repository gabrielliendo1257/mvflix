package com.guille.media.bff.experience.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.guille.media.bff.experience.catalog.application.port.CatalogProjection;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

class GetCatalogTest {

  private final CatalogProjection projection = mock(CatalogProjection.class);
  private final GetCatalog getCatalog = new GetCatalog(this.projection);

  private CatalogPage pageWith(String title, String source, Boolean assetPresent) {
    return new CatalogPage(
        new CatalogPage.Summary(1, 1, 0),
        List.of(new CatalogPage.Item(
            new CatalogPage.Key("MEDIA", 42L), 42L, null, assetPresent, title,
            "/p.jpg", 2009, "1h 40m", "MOVIE", "READY", "READY",
            source, "PRIVATE", 2, "LINKED")),
        0, 25, 1, 1);
  }

  @Test
  void returnsProjectionFromThePort() {
    when(this.projection.page(0, 25, null, null, null, null))
        .thenReturn(Mono.just(pageWith("Coraline", "MANAGED", null)));

    StepVerifier.create(this.getCatalog.execute(null, null, null, null, null, null))
        .assertNext(page -> {
          assertThat(page.total()).isEqualTo(1);
          assertThat(page.summary().ready()).isEqualTo(1);
          var item = page.items().get(0);
          assertThat(item.title()).isEqualTo("Coraline");
          assertThat(item.playable()).isTrue();
          assertThat(item.getCapabilities().play()).isTrue();
          // Scope OWNED: el dueño gestiona su ficha.
          assertThat(item.getCapabilities().delete()).isTrue();
        })
        .verifyComplete();
  }

  @Test
  void missingLocalFileKillsPlayAndDeleteCapabilities() {
    when(this.projection.page(0, 25, null, null, null, null))
        .thenReturn(Mono.just(new CatalogPage(
            new CatalogPage.Summary(1, 0, 1),
            List.of(new CatalogPage.Item(
                new CatalogPage.Key("MEDIA", 9L), 9L, 3L, Boolean.FALSE, "Alien",
                null, 1979, "1h 57m", "MOVIE", "READY", "MISSING",
                "LOCAL", "PRIVATE", 0, "NONE")),
            0, 25, 1, 1)));

    StepVerifier.create(this.getCatalog.execute(null, null, null, null, null, null))
        .assertNext(page -> {
          var caps = page.items().get(0).getCapabilities();
          assertThat(caps.play()).isFalse();
          assertThat(caps.delete()).isFalse();
          assertThat(caps.editMetadata()).isTrue();
          // MOVIE sin proveedor: se puede vincular, no desvincular.
          assertThat(caps.linkProvider()).isTrue();
          assertThat(caps.unlinkProvider()).isFalse();
        })
        .verifyComplete();
  }

  @Test
  void providerCapabilitiesFollowKindAndLinkState() {
    when(this.projection.page(0, 25, null, null, null, null))
        .thenReturn(Mono.just(new CatalogPage(
            new CatalogPage.Summary(2, 2, 0),
            List.of(
                // MOVIE + LINKED → solo unlink.
                new CatalogPage.Item(
                    new CatalogPage.Key("MEDIA", 1L), 1L, null, Boolean.TRUE,
                    "Coraline", "/c.jpg", 2009, "1h 40m", "MOVIE",
                    "READY", "READY", "LOCAL", "PRIVATE", 0, "LINKED"),
                // OTHER → nunca se vincula a proveedores.
                new CatalogPage.Item(
                    new CatalogPage.Key("MEDIA", 2L), 2L, null, Boolean.TRUE,
                    "Clip propio", null, null, "0h 30s", "OTHER",
                    "READY", "READY", "LOCAL", "PRIVATE", 0, "NONE")),
            0, 25, 2, 1)));

    StepVerifier.create(this.getCatalog.execute(null, null, null, null, null, null))
        .assertNext(page -> {
          var linked = page.items().get(0).getCapabilities();
          assertThat(linked.linkProvider()).isFalse();
          assertThat(linked.unlinkProvider()).isTrue();

          var other = page.items().get(1).getCapabilities();
          assertThat(other.linkProvider()).isFalse();
          assertThat(other.unlinkProvider()).isFalse();
        })
        .verifyComplete();
  }

  @Test
  void assetItemOnlyDerivesIdentify() {
    when(this.projection.page(0, 25, null, null, null, null))
        .thenReturn(Mono.just(new CatalogPage(
            new CatalogPage.Summary(1, 0, 1),
            List.of(new CatalogPage.Item(
                new CatalogPage.Key("ASSET", 91L), null, 91L, Boolean.TRUE,
                "video_123.mkv", null, null, null, null,
                null, "UNIDENTIFIED", "LOCAL", null, 0, null)),
            0, 25, 1, 1)));

    StepVerifier.create(this.getCatalog.execute(null, null, null, null, null, null))
        .assertNext(page -> {
          var caps = page.items().get(0).getCapabilities();
          // La única acción con endpoint real para un asset sin identificar.
          assertThat(caps.identify()).isTrue();
          // viewDetail/delete no existen: /web/media/{id} espera un movie ID.
          assertThat(caps.viewDetail()).isFalse();
          assertThat(caps.delete()).isFalse();
          assertThat(caps.play()).isFalse();
          assertThat(caps.editMetadata()).isFalse();
          assertThat(caps.changeVisibility()).isFalse();
          assertThat(caps.manageSharing()).isFalse();
          assertThat(caps.linkProvider()).isFalse();
          assertThat(caps.unlinkProvider()).isFalse();
        })
        .verifyComplete();
  }

  @Test
  void forwardsNormalizedFiltersToThePort() {
    when(this.projection.page(2, 10, "cora", "DRAFT", "title", "ASC"))
        .thenReturn(Mono.just(CatalogPage.empty()));

    this.getCatalog.execute(2, 10, "  cora ", " DRAFT ", " title ", "ASC").block();

    verify(this.projection).page(2, 10, "cora", "DRAFT", "title", "ASC");
  }

  @Test
  void blankParametersAreSentAsNullAndSizeCappedAtMax() {
    when(this.projection.page(0, GetCatalog.MAX_SIZE, null, null, null, null))
        .thenReturn(Mono.just(CatalogPage.empty()));

    this.getCatalog.execute(-1, 500, "   ", "", null, null).block();

    verify(this.projection).page(0, GetCatalog.MAX_SIZE, null, null, null, null);
  }
}
