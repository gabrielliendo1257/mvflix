package com.guille.media.bff.experience.shell.application;

/**
 * Capacidades de producto derivadas del perfil y los roles. Expresan
 * intención (canAddMedia, no canUpload); Angular decide qué navegar y cada
 * microservicio sigue aplicando su propio enforcement server-side.
 *
 * <p>Dos fuentes de verdad: la cuenta (users: enabled/blocked) y los roles
 * del token (claim {@code roles} emitido por el IdP desde su tabla roles).
 */
public record ShellCapabilities(
    boolean canAddMedia,
    boolean canManageLibraries,
    boolean canAccessAdmin,
    /** Opera bibliotecas globales del operador (storage distingue operador/propietario). */
    boolean canManageAnyLibrary,
    /** Edita/borra contenido de otros usuarios (moderación). */
    boolean canModerateCatalog,
    /** Ve la actividad de todos los usuarios, no solo la propia. */
    boolean canViewAllActivity) {

  public static ShellCapabilities none() {
    return new ShellCapabilities(false, false, false, false, false, false);
  }

  /**
   * Cuenta usable sin rol: puede añadir contenido por el camino MANAGED
   * (uploads a MinIO); las bibliotecas locales son exclusivas del operador.
   */
  public static ShellCapabilities accountOnly(boolean accountUsable) {
    return new ShellCapabilities(accountUsable, false, false, false, false, false);
  }

  public static ShellCapabilities admin(boolean accountUsable) {
    return new ShellCapabilities(
        accountUsable, accountUsable, true, true, true, true);
  }
}
