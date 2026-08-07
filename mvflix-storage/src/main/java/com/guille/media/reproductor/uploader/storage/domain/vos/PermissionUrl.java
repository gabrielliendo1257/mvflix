package com.guille.media.reproductor.uploader.storage.domain.vos;

import java.util.Map;

public record PermissionUrl(String presignedUrl, String method, Map<String, String> headers) {

}
