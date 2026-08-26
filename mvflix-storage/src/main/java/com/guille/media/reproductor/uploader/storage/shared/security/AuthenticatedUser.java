package com.guille.media.reproductor.uploader.storage.shared.security;

import java.util.Set;

/**
 * Usuario autenticado según el token o la sesión OIDC. Los roles viajan en el
 * claim {@code roles} del JWT emitido por el IdP; sustentan políticas como
 * "solo un administrador registra bibliotecas locales".
 */
public record AuthenticatedUser(String subject, String email, Set<String> roles) {

  public static final String ADMIN_ROLE = "ROLE_ADMIN";

  public AuthenticatedUser(String subject, String email) {
    this(subject, email, Set.of());
  }

  public AuthenticatedUser {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  /** Rol de administración del producto (bibliotecas locales, moderación). */
  public boolean isAdmin() {
    return this.roles.contains(ADMIN_ROLE);
  }
}
