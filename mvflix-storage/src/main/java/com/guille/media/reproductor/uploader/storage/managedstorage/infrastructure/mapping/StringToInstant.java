package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping.InvalidConversion;

@Component
public class StringToInstant implements Converter<String, Instant> {
    @Nullable
    @Override
    public Instant convert(String source) {
        try {
            return Instant.parse(source);
        } catch (DateTimeParseException e) {
            throw new InvalidConversion(
                    "Invalid Instant format.",
                    e);
        }
    }
}
