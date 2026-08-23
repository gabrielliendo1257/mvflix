package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class InstantToString implements Converter<Instant, String> {
    @Nullable
    @Override
    public String convert(Instant source) {
        return Instant.ofEpochMilli(source.toEpochMilli()).toString();
    }
}
