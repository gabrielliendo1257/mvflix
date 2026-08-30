package com.gcorp.service.app.mvflix_movies.catalog.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.MovieMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.CatalogMetadata;
import com.gcorp.service.app.mvflix_movies.catalog.domain.item.CatalogItemKind;
import com.gcorp.service.app.mvflix_movies.catalog.domain.metadata.VideoMetadata;

import org.springframework.stereotype.Component;

/**
 * Codifica/decodifica el value object {@link MovieMetadata} a JSON. La única
 * pieza de infraestructura que conoce Jackson: el dominio no depende de la
 * serialización.
 */
@Component
public class JsonCodec {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public String encode(CatalogMetadata metadata) {
        try {
            if (metadata instanceof MovieMetadata movie) {
                ObjectNode json = this.objectMapper.valueToTree(movie);
                json.remove("providerLink");
                if (movie.tmdbId() != null) {
                    json.put("tmdbId", movie.tmdbId());
                }
                return this.objectMapper.writeValueAsString(json);
            }
            return this.objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot encode movie metadata", e);
        }
    }

    public MovieMetadata decode(String json) {
        try {
            return decodeMovie(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot decode movie metadata", e);
        }
    }

    public CatalogMetadata decode(String json, CatalogItemKind kind) {
        try {
            if (kind == CatalogItemKind.VIDEO) {
                ObjectNode node = (ObjectNode) this.objectMapper.readTree(json);
                if (node.has("overview") && !node.has("description")) {
                    return new VideoMetadata(
                            node.path("title").asText(null),
                            node.path("overview").asText(null),
                            null);
                }
                VideoMetadata video = this.objectMapper.readValue(json, VideoMetadata.class);
                return video;
            }
            return decodeMovie(json);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot decode catalog metadata", e);
        }
    }

    private MovieMetadata decodeMovie(String json) throws JsonProcessingException {
        ObjectNode node = (ObjectNode) this.objectMapper.readTree(json);
        Long tmdbId = node.hasNonNull("tmdbId") ? node.get("tmdbId").longValue() : null;
        node.remove("tmdbId");
        return this.objectMapper.treeToValue(node, MovieMetadata.class).withTmdbId(tmdbId);
    }
}
