package com.guille.media.bff.experience.addmedia.application;

/** Política de users: usuario bloqueado por violaciones repetidas. */
public class UserBlockedException extends RuntimeException {

  public UserBlockedException(String username, int violations) {
    super("El usuario está bloqueado por violaciones repetidas (" + username
        + ", violaciones=" + violations + ")");
  }
}
