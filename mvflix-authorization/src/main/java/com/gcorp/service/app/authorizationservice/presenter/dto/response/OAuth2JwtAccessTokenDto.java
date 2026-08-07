package com.gcorp.service.app.authorizationservice.presenter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuth2JwtAccessTokenDto(
        @JsonProperty(value = "access_token")
        String accessToken,

        @JsonProperty(value = "refresh_token")
        String refreshToken,

        @JsonProperty(value = "token_type")
        String tokenType,

        @JsonProperty(value = "scope")
        String scope,

        @JsonProperty(value = "id_token")
        String idToken,

        @JsonProperty(value = "expires_in")
        Long expiresIn
)
{
}
