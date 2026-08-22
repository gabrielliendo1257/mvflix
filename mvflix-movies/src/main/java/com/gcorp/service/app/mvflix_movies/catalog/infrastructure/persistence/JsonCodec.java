package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcorp.service.app.mvflix_movies.catalog.domain.movie.MovieMetadata;

import org.springframework.stereotype.Component;

/**
 * Codifica/decodifica el value object {@link MovieMetadata} a JSON. La única
 * pieza de infraestructura que conoce Jackson: el dominio no depende de la
 * serialización.
 */
@Component
public class JsonCodec {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String encode(MovieMetadata metadata) {
        try {
            return this.objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot encode movie metadata", e);
        }
    }

    public MovieMetadata decode(String json) {
        try {
            return this.objectMapper.readValue(json, MovieMetadata.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot decode movie metadata", e);
        }
    }
}
