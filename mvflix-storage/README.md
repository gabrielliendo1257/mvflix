# Movie App

### Descripcion:
#### Aplicacion backend de un sitio de peliculas.



# 🔐 Configuración OAuth2 — Cliente `app-movie`

Este cliente se utiliza para la autenticación del front `MovieTV App` contra el Authorization Server.

---

## ⚙️ Configuración del cliente

```java
var registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("app-movie")
        .clientSecret(passwordEncoder.encode("super-secret"))
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.OPENID)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("http://127.0.0.1:9090/login/oauth2/movietv")
        .postLogoutRedirectUri("http://localhost:9090/")
        .tokenSettings(TokenSettings.builder()
                .reuseRefreshTokens(false)
                .accessTokenTimeToLive(Duration.ofMinutes(7))
                .refreshTokenTimeToLive(Duration.ofDays(5))
                .build())
        .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
        .build();
````

---

## 🧩 Detalles del cliente

|Propiedad|Valor|Descripción|
|---|---|---|
|**Client ID**|`app-movie`|Identificador del cliente|
|**Client Secret**|`super-secret` (encriptado con BCrypt)|Credencial privada del cliente|
|**Redirect URI**|`http://127.0.0.1:9090/login/oauth2/movietv`|Donde el usuario es redirigido tras login|
|**Logout Redirect URI**|`http://localhost:9090/`|URI de redirección post-logout|
|**Grant Types**|`authorization_code`, `refresh_token`|Tipos de flujo soportados|
|**Access Token TTL**|`7 min`|Duración del access token|
|**Refresh Token TTL**|`5 días`|Duración del refresh token|
|**Scopes**|`openid`, `profile`|Permisos del token|
|**Consent Required**|`false`|No requiere pantalla de consentimiento|

---

## 🔑 Flujo de Autenticación (Authorization Code)

### 1️⃣ Solicitar autorización

El cliente redirige al usuario al Authorization Server:

```
GET /oauth2/authorize?response_type=code
    &client_id=app-movie
    &redirect_uri=http://127.0.0.1:9090/login/oauth2/movietv
    &scope=openid profile
    &state=xyz123
```

> 💡 _El usuario inicia sesión y el servidor devuelve un `authorization_code` en la URL de redirección._

---

### 2️⃣ Obtener Access Token y Refresh Token

Una vez obtenido el `code`, se hace una llamada al endpoint de tokens:

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u app-movie:super-secret \
  -d "grant_type=authorization_code" \
  -d "redirect_uri=http://127.0.0.1:9090/login/oauth2/movietv" \
  -d "code=AUTH_CODE_AQUI"
```

📥 **Respuesta esperada:**

```json
{
  "access_token": "eyJraWQiOiJhb...",
  "refresh_token": "def50200a1...",
  "token_type": "Bearer",
  "expires_in": 420,
  "scope": "openid profile"
}
```

---

### 3️⃣ Refrescar el Token

Cuando el access token expira (7 minutos), puedes solicitar uno nuevo:

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u app-movie:super-secret \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN_AQUI"
```

📥 **Respuesta:**

```json
{
  "access_token": "nuevo_token_jwt",
  "refresh_token": "nuevo_refresh_token",
  "expires_in": 420
}
```

---

### 4️⃣ Introspección de Token

Para validar si un token sigue activo:

```bash
curl -X POST http://localhost:8080/oauth2/introspect \
  -u app-movie:super-secret \
  -d "token=ACCESS_TOKEN_AQUI"
```

📥 **Respuesta:**

```json
{
  "active": true,
  "sub": "user123",
  "scope": "openid profile",
  "exp": 1735630000
}
```

---

### 5️⃣ Logout

Para invalidar la sesión del usuario y redirigir al front:

```
GET /logout?post_logout_redirect_uri=http://localhost:9090/
```

---

## 📘 Notas adicionales

> ⚠️ **Seguridad:** nunca expongas el `client_secret` en el front.  
> 💡 **Consejo:** usa HTTPS siempre en entornos `prod`.  
> 🧠 **Tip:** puedes probar todos estos flujos en Postman con `grant_type=authorization_code` configurado.

---

## 🧰 Endpoints relevantes

|Endpoint|Método|Descripción|
|---|---|---|
|`/oauth2/authorize`|GET|Solicita autorización al usuario|
|`/oauth2/token`|POST|Intercambia código por token|
|`/oauth2/introspect`|POST|Verifica si un token es válido|
|`/logout`|GET|Cierra la sesión del usuario|

---

## 🌐 Recursos

- [Documentación oficial Spring Authorization Server](https://docs.spring.io/spring-authorization-server/)
    
- [RFC 6749: OAuth 2.0 Framework](https://datatracker.ietf.org/doc/html/rfc6749)
    
- [RFC 7662: Token Introspection](https://datatracker.ietf.org/doc/html/rfc7662)
