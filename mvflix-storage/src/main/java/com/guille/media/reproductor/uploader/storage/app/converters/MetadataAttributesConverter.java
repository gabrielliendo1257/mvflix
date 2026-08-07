package com.guille.media.reproductor.uploader.storage.app.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Converter
public class MetadataAttributesConverter implements AttributeConverter<Map<String, String>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, String> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return null;
		}

		try {
			var writerResult = OBJECT_MAPPER.writeValueAsString(attributes);
			log.info("Converted attributes to String: {}", writerResult);
			return writerResult;
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException(
					"Failed to convert attributes map to JSON", e);
		}
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String json) {
		if (json == null || json.isBlank()) {
			return Collections.emptyMap();
		}
		log.info("Converting attributes map to JSON: {}", json);

		try {
			if (json.startsWith("\"")) {
				json = OBJECT_MAPPER.readValue(json, String.class);
			}

			var value = OBJECT_MAPPER.readValue(
					json,
					new TypeReference<Map<String, String>>() {
					});
			log.info("Converted attributes map: {}", value);
			return value;
		} catch (IOException e) {
			throw new IllegalArgumentException(
					"Failed to convert JSON to attributes map", e);
		}
	}
}
