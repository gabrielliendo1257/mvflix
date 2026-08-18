package com.guille.media.bff.app.dto;

/** Biblioteca del operador visible por el front (nunca su root completo). */
public record LibraryDto(Long id, String type, boolean enabled) {}
