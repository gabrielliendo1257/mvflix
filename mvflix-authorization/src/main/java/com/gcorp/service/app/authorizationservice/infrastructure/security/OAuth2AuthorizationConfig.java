package com.gcorp.service.app.authorizationservice.infrastructure.security;

import java.time.Duration;

import com.gcorp.service.app.authorizationservice.infrastructure.security.props.OAuth2PropertiesConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class OAuth2AuthorizationConfig {

	private final OAuth2PropertiesConfig oauth2PropertiesConfig;

	@Bean
	@Order(value = Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http)
			throws Exception {
		OAuth2AuthorizationServerConfigurer oAuthorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

		http.securityMatcher(
				oAuthorizationServerConfigurer.getEndpointsMatcher())
				.exceptionHandling(exceptionConfig -> exceptionConfig.defaultAuthenticationEntryPointFor(
						new LoginUrlAuthenticationEntryPoint("/login"),
						new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
				.with(oAuthorizationServerConfigurer,
						authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
				.authorizeHttpRequests(authorizeConfig -> authorizeConfig.anyRequest().authenticated());

		return http.build();
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(
			JdbcTemplate jdbcTemplate,
			PasswordEncoder passwordEncoder) {
		var repository = new JdbcRegisteredClientRepository(jdbcTemplate);

		if (repository.findByClientId(this.oauth2PropertiesConfig.getOauth2ClientId()) == null) {
			var movieFrontRegisteredClient = RegisteredClient.withId(
					this.oauth2PropertiesConfig.getRegistrationId())
					.clientId(this.oauth2PropertiesConfig.getOauth2ClientId())
					.clientSecret(
							passwordEncoder.encode(
									this.oauth2PropertiesConfig.getFrontClientSecret()))
					.scope(OidcScopes.PROFILE)
					.scope(OidcScopes.OPENID)
					.clientAuthenticationMethod(
							ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
					.redirectUri(this.oauth2PropertiesConfig.getOauth2Redirect())
					.postLogoutRedirectUri(
							this.oauth2PropertiesConfig.getOauth2LogOutRedirect())
					.tokenSettings(
							TokenSettings.builder()
									.reuseRefreshTokens(false)
									.accessTokenTimeToLive(Duration.ofMinutes(7))
									.refreshTokenTimeToLive(Duration.ofDays(5))
									.build())
					.clientSettings(
							ClientSettings.builder()
									.requireAuthorizationConsent(false)
									.requireProofKey(true)
									.build())
					.build();
			repository.save(movieFrontRegisteredClient);
		}

		if (repository.findByClientId("storage-service") == null) {
			var storageServiceRegisteredClient = RegisteredClient.withId("storage-app")
					.clientId("storage-service")
					.clientSecret(passwordEncoder.encode(
							this.oauth2PropertiesConfig.getFrontClientSecret()))
					.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
					.scope("users.read")
					.scope("users.write")
					.build();
			repository.save(storageServiceRegisteredClient);
		}

		return repository;
	}

	@Bean
	OAuth2AuthorizationService authorizationService(
			JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	OAuth2AuthorizationConsentService authorizationConsentService(
			JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
	}

	@Bean
	WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers("/h2-console/**");
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}
}
