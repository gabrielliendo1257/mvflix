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
}
