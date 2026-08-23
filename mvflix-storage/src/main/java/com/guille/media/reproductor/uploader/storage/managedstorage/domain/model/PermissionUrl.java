package com.guille.media.reproductor.uploader.storage.managedstorage.domain.model;

import java.util.Map;

public record PermissionUrl(String presignedUrl, String method, Map<String, String> headers) {

}
