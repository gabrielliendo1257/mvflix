package com.guille.media.reproductor.uploader.storage.app.converters;

import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.guille.media.reproductor.uploader.storage.domain.vos.MimeType;

@Component
public class StringToMimeType implements Converter<String, MimeType> {
    @Nullable
    @Override
    public MimeType convert(String source) {
        return new MimeType(source);
    }
}
