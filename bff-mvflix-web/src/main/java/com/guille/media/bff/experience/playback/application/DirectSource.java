package com.guille.media.bff.experience.playback.application;

import java.time.Instant;

/**
 * Acceso directo al contenido: una URL auto-autenticada (presigned MinIO o
 * capability del proxy LOCAL) que el reproductor consume con Range requests.
 * El BFF no transmite bytes por aqui.
 *
 * <p>Cuando exista HLS/transcode, esta clase se promueve a sealed interface
 * con variantes; hoy una sola forma evita una jerarquia especulativa.
 */
public record DirectSource(String url, Instant expiresAt, String mimeType) {}
