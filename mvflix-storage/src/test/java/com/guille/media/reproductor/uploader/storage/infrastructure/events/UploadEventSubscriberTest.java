package com.guille.media.reproductor.uploader.storage.infrastructure.events;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.guille.media.reproductor.uploader.storage.app.user.UserServiceCommandPort;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadCompletedEvent;
import com.guille.media.reproductor.uploader.storage.domain.events.UploadFailedEvent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class UploadEventSubscriberTest {

  private final UserServiceCommandPort userServiceCommandPort = mock(UserServiceCommandPort.class);
  private final UploadEventSubscribers subscriber =
      new UploadEventSubscribers(userServiceCommandPort);

  @Test
  void completedEventNotifiesUserServiceWithConsumedBytes() {
    UploadCompletedEvent event =
        new UploadCompletedEvent(
            7L, "pepe", "pepe/videos/a.mp4", "video/mp4", 1024L, Instant.now());

    this.subscriber.onUploadCompleted(event);

    verify(this.userServiceCommandPort).applyQuota("pepe", 1024L);
  }

  @Test
  void failedEventDoesNotNotifyQuota() {
    UploadFailedEvent event =
        new UploadFailedEvent(
            7L, "pepe", "pepe/videos/a.mp4", "Object size mismatch for upload: 7", Instant.now());

    this.subscriber.onUploadFailed(event);

    verify(this.userServiceCommandPort, never()).applyQuota(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  void failedEventIsDeliveredToTheUserViaStatusOutsideTheService() {
    UploadFailedEvent event =
        new UploadFailedEvent(
            7L, "pepe", "pepe/videos/a.mp4", "Object size mismatch for upload: 7", Instant.now());

    this.subscriber.onUploadFailed(event);
  }
}
