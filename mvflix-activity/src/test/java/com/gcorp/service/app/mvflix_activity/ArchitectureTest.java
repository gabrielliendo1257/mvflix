package com.gcorp.service.app.mvflix_activity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ArchitectureTest {
  @Test void coreTypesRemainOutsideInfrastructure() {
    assertThat(com.gcorp.service.app.mvflix_activity.domain.PlaybackProgressed.class.getPackageName()).doesNotContain("infrastructure");
    assertThat(com.gcorp.service.app.mvflix_activity.application.ActivityProcessor.class.getPackageName()).doesNotContain("infrastructure");
    assertThat(com.gcorp.service.app.mvflix_activity.application.port.ActivityInbox.class.getPackageName()).doesNotContain("infrastructure");
  }
}
