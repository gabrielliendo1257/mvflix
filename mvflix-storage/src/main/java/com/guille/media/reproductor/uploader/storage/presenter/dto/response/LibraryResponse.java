package com.guille.media.reproductor.uploader.storage.presenter.dto.response;

/**
 * Biblioteca del media server visible por la API (el root completo no se expone
 * a usuarios; el front solo ve id/type/enabled).
 */
public record LibraryResponse(
    Long id, String type, boolean enabled, String rootPath) {}