package com.guille.media.reproductor.uploader.storage.app.user;

public record UserQuota(
                String plan,
                Long maxUploadSize,
                Long remainingBytes) {
}
