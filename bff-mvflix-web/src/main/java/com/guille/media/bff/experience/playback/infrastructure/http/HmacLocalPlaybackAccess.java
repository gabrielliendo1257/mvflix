package com.guille.media.bff.experience.playback.infrastructure.http;

import com.guille.media.bff.experience.playback.application.LocalStreamTokenException;
import com.guille.media.bff.experience.playback.application.port.LocalPlaybackAccess;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Capability HMAC para el stream LOCAL del proxy del BFF. A diferencia del
 * ticket legacy, NO lleva el JWT del usuario: solo liga (media, asset,
 * biblioteca, ruta) con expiración y sujeto; las credenciales reales se
 * resuelven desde la sesión OAuth2 viva en cada Range request.
 *
 * <p>TTL por defecto de 2h (cubre una película); es un token por asset,
 * no una credencial general.
 */
@Slf4j
@Component
public class HmacLocalPlaybackAccess implements LocalPlaybackAccess {

  private final byte[] secret;
  private final Duration ttl;

  public HmacLocalPlaybackAccess(
      @Value("${bff.stream-ticket.secret:dev-stream-ticket-secret}") String secret,
      @Value("${bff.playback.local-access-ttl:PT2H}") Duration ttl) {
    this.secret = sha256(secret);
    this.ttl = ttl;
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes());
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 no disponible", error);
    }
  }

  @Override
  public Mono<MintedAccess> mint(LocalMintCommand command) {
    return Mono.fromCallable(() -> this.sign(command));
  }

  private MintedAccess sign(LocalMintCommand command) {
    Instant expiresAt = Instant.now().plus(this.ttl.getSeconds(), ChronoUnit.SECONDS);
    try {
      var claims = new JWTClaimsSet.Builder()
          .subject(String.valueOf(command.mediaId()))
          .claim("assetId", command.assetId())
          .claim("libraryId", command.libraryId())
          .claim("relPath", command.relativePath())
          .claim("subject", command.subject())
          .expirationTime(Date.from(expiresAt))
          .build();
      var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(this.secret));
      return new MintedAccess(jwt.serialize(), expiresAt);
    } catch (JOSEException error) {
      throw new IllegalStateException("No se pudo firmar el acceso local de playback", error);
    }
  }

  @Override
  public Mono<LocalGrant> resolve(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return Mono.error(new LocalStreamTokenException("Acceso de stream ausente"));
    }
    return Mono.fromCallable(() -> this.verify(rawToken));
  }

  private LocalGrant verify(String rawToken) {
    try {
      SignedJWT jwt = SignedJWT.parse(rawToken);
      if (!jwt.verify(new MACVerifier(this.secret))) {
        throw new LocalStreamTokenException("Acceso de stream inválido");
      }
      var claims = jwt.getJWTClaimsSet();
      Date expiresAt = claims.getExpirationTime();
      if (expiresAt == null || expiresAt.toInstant().isBefore(Instant.now())) {
        throw new LocalStreamTokenException("Acceso de stream expirado");
      }
      return new LocalGrant(
          Long.parseLong(claims.getSubject()),
          claims.getLongClaim("assetId"),
          claims.getLongClaim("libraryId"),
          claims.getStringClaim("relPath"),
          claims.getStringClaim("subject"),
          expiresAt.toInstant());
    } catch (ParseException | JOSEException | NumberFormatException | NullPointerException error) {
      // NullPointerException cubre claims obligatorios ausentes: sigue siendo
      // un token malformado, no un fallo del servidor.
      throw new LocalStreamTokenException("Acceso de stream inválido");
    }
  }
}
