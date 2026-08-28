package com.gcorp.service.app.authorizationservice.infrastructure.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerMapper;
import com.gcorp.service.app.authorizationservice.infrastructure.persistence.jpa.customer.CustomerRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ActuatorSecurityTest.Endpoint.class)
@Import({ActuatorSecurityConfig.class, ActuatorSecurityTest.Endpoint.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = {
    "ACTUATOR_METRICS_USER=metrics",
    "ACTUATOR_METRICS_PASSWORD=change-me"
})
class ActuatorSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CustomerRepository customerRepository;
    @MockBean private CustomerMapper customerMapper;

    @Test
    void actuatorRequiresValidBasicAuth() throws Exception {
        this.mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/actuator/prometheus").with(httpBasic("metrics", "wrong")))
                .andExpect(status().isUnauthorized());
        this.mockMvc.perform(get("/actuator/prometheus").with(httpBasic("metrics", "change-me")))
                .andExpect(status().isOk());
    }

    @RestController
    public static class Endpoint {
        @GetMapping("/actuator/prometheus")
        String prometheus() {
            return "# HELP test_metric 1\n";
        }
    }
}
