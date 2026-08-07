package com.guille.media.reproductor.uploader.storage.domain.vos;

import java.util.concurrent.TimeUnit;

public record PresignedUrl(
                String url,
                int expire,
                TimeUnit timeUnit,
                Object httpMethod) {
}
