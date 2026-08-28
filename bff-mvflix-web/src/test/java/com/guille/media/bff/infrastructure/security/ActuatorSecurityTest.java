package com.guille.media.bff.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebFluxTest(controllers = ActuatorSecurityTest.Endpoint.class)
@Import({SandboxWebSecurityConfig.class, ActuatorSecurityTest.Endpoint.class})
@ActiveProfiles("sandbox")
@TestPropertySource(properties = {
    "ACTUATOR_METRICS_USER=metrics",
    "ACTUATOR_METRICS_PASSWORD=change-me"
})
class ActuatorSecurityTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  void actuatorRequiresValidBasicAuth() {
    this.webTestClient.get().uri("/actuator/prometheus").exchange()
        .expectStatus().isUnauthorized();
    this.webTestClient.get().uri("/actuator/prometheus")
        .headers(headers -> headers.setBasicAuth("metrics", "wrong"))
        .exchange().expectStatus().isUnauthorized();
    this.webTestClient.get().uri("/actuator/prometheus")
        .headers(headers -> headers.setBasicAuth("metrics", "change-me"))
        .exchange().expectStatus().isOk();
  }

  @RestController
  public static class Endpoint {
    @GetMapping("/actuator/prometheus")
    String prometheus() {
      return "# HELP test_metric 1\n";
    }
  }
}
