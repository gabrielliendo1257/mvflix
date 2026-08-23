package com.guille.media.reproductor.uploader.storage.managedstorage.infrastructure.mapping;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StorageKey;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StorageKeyToString implements Converter<StorageKey, String> {

	@Nullable
	@Override
	public String convert(StorageKey source) {
		return source.key();
	}
}
