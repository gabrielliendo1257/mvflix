package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import java.time.Instant;

import com.guille.media.reproductor.uploader.storage.managedstorage.domain.model.StoreObject.StorageSessionStatus;

public record ExpectedObjectData(long expectedSize, String expectedMime) {}
