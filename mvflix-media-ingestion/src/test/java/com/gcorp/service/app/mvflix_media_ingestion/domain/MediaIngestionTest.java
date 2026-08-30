package com.gcorp.service.app.mvflix_media_ingestion.domain;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MediaIngestionTest {
  @Test void transitionUsesOptimisticVersionAndRejectsTerminalState() {
    var now=Instant.now(); var i=new MediaIngestion(UUID.randomUUID(),"actor",null,null,MediaIngestion.Phase.STARTING,null,4,0,now,now,now,"key","x.mp4",10,"video/mp4",null);
    var next=i.transition(MediaIngestion.Phase.PREPARING_CATALOG,12L,null,null);
    assertEquals(5,next.version()); assertEquals(12L,next.catalogItemId());
    var done=next.transition(MediaIngestion.Phase.COMPLETED,null,"u",null);
    assertThrows(IllegalStateException.class,()->done.transition(MediaIngestion.Phase.CANCELLING,null,null,null));
  }
}
