package com.guille.media.bff.app.dto;

/** Identificación de un activo; el título es opcional (se deriva del filename). */
public record IdentifyAssetRequest(String title) {}
