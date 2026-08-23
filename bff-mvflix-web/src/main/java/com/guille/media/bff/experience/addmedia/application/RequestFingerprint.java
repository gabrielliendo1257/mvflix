package com.guille.media.bff.experience.addmedia.application;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Huella estable de la intención del usuario para la misma idempotencyKey:
 * SHA-256 del payload canónico (sin incluir la propia key). Si la key se
 * reutiliza con OTRO payload, el alta debe rechazarse en lugar de devolver
 * silenciosamente el proceso anterior.
 */
public final class RequestFingerprint {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private RequestFingerprint() {}

  public static String of(Object payload) {
    try {
      byte[] canonical = MAPPER.writeValueAsBytes(payload);
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo calcular el fingerprint", e);
    }
  }

  public static String utf8(String value) {
    return value == null ? "" : value;
  }

  static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }
}
