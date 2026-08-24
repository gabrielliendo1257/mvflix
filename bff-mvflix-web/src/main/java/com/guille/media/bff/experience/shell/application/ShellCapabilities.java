package com.guille.media.bff.experience.shell.application;

/**
 * Capacidades de producto derivadas del perfil y los roles. Expresan
 * intención (canAddMedia, no canUpload); Angular decide qué navegar.
 */
public record ShellCapabilities(
    boolean canAddMedia,
    boolean canManageLibraries,
    boolean canAccessAdmin) {

  public static ShellCapabilities none() {
    return new ShellCapabilities(false, false, false);
  }
}
