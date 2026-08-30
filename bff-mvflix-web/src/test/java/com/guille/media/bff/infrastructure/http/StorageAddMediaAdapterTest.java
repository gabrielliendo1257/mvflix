package com.guille.media.bff.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guille.media.bff.app.dto.UploadSessionDto;
import com.guille.media.bff.app.ports.StorageWebClient;
import com.guille.media.bff.experience.addmedia.application.DownstreamRejectionException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class StorageAddMediaAdapterTest {
  private final StorageWebClient delegate = mock(StorageWebClient.class);
  private final StorageAddMediaAdapter adapter = new StorageAddMediaAdapter(delegate);
  private final UploadSessionDto session = new UploadSessionDto("42", null, "key", null,
      "PENDING", null);

  @Test
  void lookupReturnsThePendingSession() {
    when(delegate.findUploadByIdempotencyKey("idem")).thenReturn(Mono.just(session));

    assertThat(adapter.recoverUpload("owner", "idem").block()).isEqualTo(session);
  }

  @Test
  void lookup404IsAnEmptyRecoveryResult() {
    when(delegate.findUploadByIdempotencyKey("idem"))
        .thenReturn(Mono.error(new DownstreamRejectionException(404, "missing")));

    assertThat(adapter.recoverUpload("owner", "idem").block()).isNull();
  }
}
