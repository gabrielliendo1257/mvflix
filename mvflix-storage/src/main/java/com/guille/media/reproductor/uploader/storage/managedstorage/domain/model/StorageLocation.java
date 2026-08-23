package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

public record StorageLocation(
		BucketName bucket,
		StorageKey storageKey) {

}
