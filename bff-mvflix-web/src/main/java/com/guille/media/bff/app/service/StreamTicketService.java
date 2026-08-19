package com.guille.media.bff.app.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Ticket de streaming firmado por el BFF: el {@code <video>} del front no puede
 * mandar el JWT del usuario en un header, asi que el front pide un ticket por
 * request autenticado y lo usa como query param en la URL del stream.
 * El ticket es HMAC (secreto del BFF), de vida corta y ligado a una movie; el
 * JWT del usuario viaja dentro para que el BFF pueda llamar a los backends.
 */
@Service
public class StreamTicketService {

  private final byte[] secret;
  private final long ttlSeconds;

  public StreamTicketService(
      @Value("${bff.stream-ticket.secret:dev-stream-ticket-secret}") String secret,
      @Value("${bff.stream-ticket.ttl-seconds:300}") long ttlSeconds) {
    this.secret = sha256(secret);
    this.ttlSeconds = ttlSeconds;
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(value.getBytes());
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 no disponible", error);
    }
  }

  public String issue(Long movieId, String userJwt) {
    Instant expiresAt = Instant.now().plus(this.ttlSeconds, ChronoUnit.SECONDS);
    try {
      var claims = new JWTClaimsSet.Builder()
          .subject(String.valueOf(movieId))
          .claim("jwt", userJwt)
          .expirationTime(Date.from(expiresAt))
          .build();
      var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
      jwt.sign(new MACSigner(this.secret));
      return jwt.serialize();
    } catch (JOSEException error) {
      throw new IllegalStateException("No se pudo firmar el ticket de stream", error);
    }
  }

  public StreamTicket resolve(String ticket) {
    try {
      SignedJWT jwt = SignedJWT.parse(ticket);
      if (!jwt.verify(new MACVerifier(this.secret))) {
        throw new StreamTicketException("Ticket de stream inválido");
      }
      JWTClaimsSet claims = jwt.getJWTClaimsSet();
      Date expiresAt = claims.getExpirationTime();
      if (expiresAt == null || expiresAt.toInstant().isBefore(Instant.now())) {
        throw new StreamTicketException("Ticket de stream expirado");
      }
      return new StreamTicket(
          Long.parseLong(claims.getSubject()),
          claims.getStringClaim("jwt"),
          expiresAt.toInstant());
    } catch (ParseException | JOSEException | NumberFormatException error) {
      throw new StreamTicketException("Ticket de stream inválido");
    }
  }
}