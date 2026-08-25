package com.guille.media.bff.experience.playback.application;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * La respuesta de "quiero reproducir este contenido ahora": identidad de la
 * sesion para correlacion (logs/trazas/progreso futuro), datos minimos de la
 * media para pintar el player, el acceso al contenido y la posicion de
 * reanudacion.
 *
 * <p>Es un record de aplicacion, NO un agregado: hoy es stateless (el
 * sessionId no se persiste ni se valida despues). Cuando aparezcan sesiones
 * activas o analytics, este tipo es el punto natural de extension.
 */
public record PlaybackSession(
    UUID sessionId,
    long mediaId,
    String title,
    String posterPath,
    String duration,
    PlaybackStrategy strategy,
    DirectSource source,
    Duration resumePosition) {}
