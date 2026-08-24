package com.guille.media.bff.experience.shell.web;

import com.guille.media.bff.experience.shell.application.ShellActivity;
import com.guille.media.bff.experience.shell.application.ShellCapabilities;
import com.guille.media.bff.experience.shell.application.ShellContext;
import com.guille.media.bff.experience.shell.application.ShellUser;

/** Forma HTTP del bootstrap; nulls explícitos para usuario anónimo. */
public record ShellResponse(
    boolean authenticated,
    ShellUser user,
    ShellCapabilities capabilities,
    ShellActivity activity) {

  public static ShellResponse from(ShellContext context) {
    return new ShellResponse(
        context.authenticated(),
        context.user(),
        context.capabilities(),
        context.activity());
  }
}
