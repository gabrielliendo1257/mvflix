package com.guille.media.reproductor.uploader.storage.domain.vos;

import java.security.SecureRandom;

public record UploadId() {
	private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final Integer LENGTH_UPLOAD_ID = 10;
	private static final SecureRandom random = new SecureRandom();

	public static String generate() {
		StringBuilder sb = new StringBuilder(LENGTH_UPLOAD_ID);

		for (int i = 0; i < LENGTH_UPLOAD_ID; i++) {
			int index = random.nextInt(CHAR_POOL.length());
			sb.append(CHAR_POOL.charAt(index));
		}

		return sb.toString();
	}
}
