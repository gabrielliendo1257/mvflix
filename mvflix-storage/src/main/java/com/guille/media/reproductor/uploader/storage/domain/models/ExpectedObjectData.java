package com.guille.media.reproductor.uploader.storage.domain.models;

import java.time.Instant;

import com.guille.media.reproductor.uploader.storage.domain.models.StoreObject.StorageSessionStatus;

public record ExpectedObjectData(long expectedSize, String expectedMime) {}
