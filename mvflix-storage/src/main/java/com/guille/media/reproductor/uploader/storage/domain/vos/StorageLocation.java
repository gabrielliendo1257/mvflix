package com.guille.media.reproductor.uploader.storage.domain.vos;

public record StorageLocation(
		BucketName bucket,
		StorageKey storageKey) {

}
