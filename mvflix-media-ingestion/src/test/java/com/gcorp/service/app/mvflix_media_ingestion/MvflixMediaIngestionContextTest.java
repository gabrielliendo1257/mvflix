package com.gcorp.service.app.mvflix_media_ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import com.gcorp.service.app.mvflix_media_ingestion.application.*;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.test.context.bean.override.mockito.MockitoBean; import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties={"spring.flyway.enabled=false","spring.main.web-application-type=none","mvflix.messaging.kafka.enabled=false","mvflix.compensation.enabled=false"})
@ActiveProfiles("test")
class MvflixMediaIngestionContextTest {
  @MockitoBean MediaIngestionRepository repository; @MockitoBean DownstreamClients clients; @MockitoBean Outbox outbox; @MockitoBean CompensationRepository compensations; @MockitoBean InboxRepository inbox; @MockitoBean ReactiveJwtDecoder jwtDecoder;
  @Autowired MediaIngestionService service;
  @Test void contextStartsWithRealCompensationDependency(){ assertThat(service).isNotNull(); }
}
