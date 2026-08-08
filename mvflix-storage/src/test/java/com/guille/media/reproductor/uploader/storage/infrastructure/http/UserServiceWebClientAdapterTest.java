package com.guille.media.reproductor.uploader.storage.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class UserServiceWebClientAdapterTest {

    private MockWebServer server;
    private UserServiceWebClientAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        this.server = new MockWebServer();
        this.server.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(this.server.url("/").toString())
                .build();
        this.adapter = new UserServiceWebClientAdapter(webClient);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.shutdown();
    }

    @Test
    void applyQuotaPostsSubjectAndQuotaAsQueryParams() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(200));

        this.adapter.applyQuota("user-1", 5_242_880L);

        RecordedRequest request = this.server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("POST", request.getMethod());
        assertEquals("/?subject=user-1&quota=5242880", request.getPath());
    }

    @Test
    void appliesQuotaWithoutThrowingOnServerError() throws Exception {
        this.server.enqueue(new MockResponse().setResponseCode(500));

        this.adapter.applyQuota("user-2", 1L);

        RecordedRequest request = this.server.takeRequest(5, TimeUnit.SECONDS);
        assertTrue(request.getPath().contains("subject=user-2"));
        assertTrue(request.getPath().contains("quota=1"));
    }
}