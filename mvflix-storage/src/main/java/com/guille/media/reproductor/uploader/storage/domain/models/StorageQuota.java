package com.guille.media.reproductor.uploader.storage.domain.models;

import lombok.Getter;

public class StorageQuota {
	@Getter
	private long userBytesQuota;

	private static final long KB = 1024L;
	private static final long MB = KB * 1024L;
	private static final long GB = MB * 1024L;

	private static final long MAX_UPLOAD_SIZE = 100L * GB;
	private static final long ENTERPRISE_MAX_UPLOAD_SIZE = 50L * GB;
	private static final long FREE_MAX_UPLOAD_SIZE = 500L * MB;

	public StorageQuota(long userBytesQuota) {
		this.userBytesQuota = userBytesQuota;
	}

	public Boolean isExceeded(StorageUsage storageUsage) {
		return userBytesQuota < storageUsage.getCurrentBytesUsage();
	}

	public static StorageQuota getQuota(Plan plan) {
		return switch (plan) {
			case FREE -> new StorageQuota(FREE_MAX_UPLOAD_SIZE);
			case ENTERPRISE -> new StorageQuota(ENTERPRISE_MAX_UPLOAD_SIZE);
			case PRO -> new StorageQuota(MAX_UPLOAD_SIZE);
			default -> throw new RuntimeException("Plan no permitido."); // TODO Cambiar a una excepcion propia
		};
	}
}
