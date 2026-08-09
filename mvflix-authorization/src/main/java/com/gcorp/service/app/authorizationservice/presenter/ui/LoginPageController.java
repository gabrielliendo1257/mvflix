package com.gcorp.service.app.authorizationservice.presenter.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página de login servida por el IdP (invitado entra aquí vía redirect de la BFF). */
@Controller
public class LoginPageController {

  @GetMapping("/login")
  public String login() {
    return "login";
  }
}