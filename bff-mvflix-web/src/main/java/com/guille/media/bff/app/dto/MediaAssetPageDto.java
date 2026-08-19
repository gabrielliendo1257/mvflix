package com.guille.media.bff.app.dto;

import java.util.List;

/** Página de media assets: los ítems del slice más metadatos para navegar. */
public record MediaAssetPageDto(
    List<MediaAssetDto> items, long total, int page, int size, int totalPages) {}