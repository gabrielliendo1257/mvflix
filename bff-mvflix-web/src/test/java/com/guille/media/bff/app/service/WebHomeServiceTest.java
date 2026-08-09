package com.guille.media.bff.app.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.QuotaSnapshot;
import com.guille.media.bff.app.dto.UploadListItem;
import com.guille.media.bff.app.dto.UserProfile;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.app.ports.UsersWebPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class WebHomeServiceTest {

  private final UsersWebPort usersWebClient = mock(UsersWebPort.class);
  private final StorageWebClient storageWebClient = mock(StorageWebClient.class);
  private WebHomeService service;

  @BeforeEach
  void setUp() {
    this.service = new WebHomeService(this.usersWebClient, this.storageWebClient);
  }

  @Test
  void homeCombinesProfileQuotaAndRecentUploads() {
    when(usersWebClient.me())
        .thenReturn(Mono.just(new UserProfile("1", "pepe", "pepe@mvflix.dev", "PRO", true)));
    when(storageWebClient.quota())
        .thenReturn(Mono.just(new QuotaSnapshot("pepe", 1_073_741_824L, 2048L, 1_073_739_776L)));
    when(storageWebClient.listUploads(10))
        .thenReturn(
            Flux.just(
                new UploadListItem(1L, "pepe/videos/a.mp4", "COMPLETED", 1024L, "2026-01-01T00:00:00Z")));

    StepVerifier.create(service.home())
        .assertNext(
            home -> {
              org.assertj.core.api.Assertions.assertThat(home.profile().username()).isEqualTo("pepe");
              org.assertj.core.api.Assertions.assertThat(home.quota().quotaBytes()).isEqualTo(1_073_741_824L);
              org.assertj.core.api.Assertions.assertThat(home.recentUploads())
                  .extracting(UploadListItem::storageId)
                  .containsExactly(1L);
            })
        .verifyComplete();
  }
}