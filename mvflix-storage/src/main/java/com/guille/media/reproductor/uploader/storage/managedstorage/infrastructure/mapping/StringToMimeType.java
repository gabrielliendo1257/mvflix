package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.MimeType;

@Component
public class StringToMimeType implements Converter<String, MimeType> {
    @Nullable
    @Override
    public MimeType convert(String source) {
        return new MimeType(source);
    }
}
