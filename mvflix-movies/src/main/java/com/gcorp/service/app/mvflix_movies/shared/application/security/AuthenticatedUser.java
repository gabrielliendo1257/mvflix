package com.gcorp.service.app.mvflix_movies.shared.application.security;

import java.util.Set;

/**
 * Usuario autenticado según el token. Los roles viajan en el claim {@code roles}
 * del JWT emitido por el IdP (desde su tabla roles); son la base de políticas de
 * producto como la moderación del catálogo. La pertenencia al catálogo sigue
 * decidiéndola {@code Movie.isOwnedBy}.
 */
public record AuthenticatedUser(String subject, String email, Set<String> roles) {

  public static final String ADMIN_ROLE = "ROLE_ADMIN";

  public AuthenticatedUser(String subject, String email) {
    this(subject, email, Set.of());
  }

  public AuthenticatedUser {
    roles = roles == null ? Set.of() : Set.copyOf(roles);
  }

  /** Rol de administración del producto (moderación, bibliotecas globales). */
  public boolean isAdmin() {
    return this.roles.contains(ADMIN_ROLE);
  }
}
