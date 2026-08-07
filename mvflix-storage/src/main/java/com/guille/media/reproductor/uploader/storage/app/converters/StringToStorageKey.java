package com.guille.media.reproductor.uploader.storage.app.converters;

import com.guille.media.reproductor.uploader.storage.domain.vos.StorageKey;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToStorageKey implements Converter<String, StorageKey> {
    @Nullable
    @Override
    public StorageKey convert(String source) {
        return new StorageKey(source);
    }
}
