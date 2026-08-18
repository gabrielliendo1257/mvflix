package com.gcorp.service.app.authorizationservice.presenter.api;

import java.time.Instant;
import java.util.List;

import com.gcorp.service.app.authorizationservice.presenter.dto.request.DevTokenRequest;
import com.gcorp.service.app.authorizationservice.presenter.dto.response.DevTokenResponse;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Emisión de tokens SOLO para desarrollo (perfil dev): valida las credenciales
 * contra el mismo {@link AuthenticationManager} del login y firma el JWT con la
 * misma JWK de {@code /oauth2/jwks}, así todos los resource servers lo validan
 * igual que uno real. Sirve para probar desde Postman sin pasar por el navegador.
 */
@Slf4j
@RestController
@Profile("dev")
public class DevTokenController {

    private static final long DEV_TOKEN_TTL_SECONDS = 7200;

    private static final List<String> DEV_SCOPES = List.of("users.read", "users.write", "openid");

    private static final String FALLBACK_ISSUER = "http://127.0.0.1:9090";

    private final AuthenticationManager authenticationManager;

    private final JWKSource<SecurityContext> jwkSource;

    private final AuthorizationServerSettings authorizationServerSettings;

    @Value("${spring.application.name}")
    private String applicationName;

    public DevTokenController(
            AuthenticationManager authenticationManager,
            JWKSource<SecurityContext> jwkSource,
            AuthorizationServerSettings authorizationServerSettings) {
        this.authenticationManager = authenticationManager;
        this.jwkSource = jwkSource;
        this.authorizationServerSettings = authorizationServerSettings;
    }

    @PostMapping(
            value = "/oauth2/dev-token",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DevTokenResponse> devToken(@RequestBody DevTokenRequest request) {
        Authentication authentication;
        try {
            authentication = this.authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));
        } catch (BadCredentialsException e) {
            log.warn("dev-token: credenciales invalidas para {}", request.username());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JwtEncoder encoder = new NimbusJwtEncoder(this.jwkSource);
        Instant now = Instant.now();
        String issuer = this.authorizationServerSettings.getIssuer();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer == null ? FALLBACK_ISSUER : issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(DEV_TOKEN_TTL_SECONDS))
                .subject(authentication.getName())
                .audience(List.of(this.applicationName))
                .claim("scope", String.join(" ", DEV_SCOPES))
                .claim("roles", authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        log.info("dev-token emitido para {} (roles={}, ttl={}s)",
                authentication.getName(), claims.getClaim("roles"), DEV_TOKEN_TTL_SECONDS);
        return ResponseEntity.ok(new DevTokenResponse(
                token, "Bearer", DEV_TOKEN_TTL_SECONDS, String.join(" ", DEV_SCOPES)));
    }
}