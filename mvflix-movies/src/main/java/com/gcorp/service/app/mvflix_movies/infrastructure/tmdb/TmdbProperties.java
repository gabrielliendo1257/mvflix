package com.gcorp.service.app.mvflix_movies.infrastructure.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(String apiToken, String baseUrl, String imageBaseUrl) {

    public boolean tokenConfigured() {
        return this.apiToken != null && !this.apiToken.isBlank();
    }
}