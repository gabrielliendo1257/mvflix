package com.guille.media.bff.experience.shell.application;

import org.springframework.lang.Nullable;

/**
 * Contexto transversal para construir la navegación: quién usa la app y qué
 * puede hacer. NO contiene rutas/labels/iconos (eso es de Angular) ni reglas
 * de Movies/Storage/Libraries.
 */
public record ShellContext(
    boolean authenticated,
    @Nullable ShellUser user,
    ShellCapabilities capabilities,
    @Nullable ShellActivity activity,
    @Nullable ShellQuota quota) {

  public static ShellContext anonymous() {
    return new ShellContext(false, null, ShellCapabilities.none(), null, null);
  }
}
