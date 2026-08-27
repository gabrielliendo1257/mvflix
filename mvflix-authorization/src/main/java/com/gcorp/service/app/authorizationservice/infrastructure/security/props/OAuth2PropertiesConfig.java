package com.gcorp.service.app.authorizationservice.infrastructure.security.props;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Getter
@Configuration
public class OAuth2PropertiesConfig {
    @Value("${authorization.env.oauth2.redirect}")
    private String oauth2Redirect;

    @Value("${authorization.env.oauth2.logout-redirect}")
    private String oauth2LogOutRedirect;

    @Value("${authorization.env.oauth2.client-id}")
    private String oauth2ClientId;

    @Value("${authorization.env.oauth2.client-password}")
    private String frontClientSecret;

    @Value("${authorization.env.oauth2.registration-id}")
    private String registrationId;

    /**
     * Secret del machine-client {@code movies-catalog} (Movies → Storage para
     * limpieza de objetos MANAGED). Dedicado: NO reutiliza el del usuario ni
     * el de playback; llega por variable de entorno {@code MOVIES_CATALOG_SECRET}.
     */
    @Value("${authorization.env.oauth2.movies-catalog-password}")
    private String moviesCatalogSecret;
}
