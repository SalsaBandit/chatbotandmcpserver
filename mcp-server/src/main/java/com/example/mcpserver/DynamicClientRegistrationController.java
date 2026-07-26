package com.example.mcpserver;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
public class DynamicClientRegistrationController {
    private final DynamicRegisteredClientRepository clients;
    private final PasswordEncoder passwordEncoder;

    public DynamicClientRegistrationController(
            DynamicRegisteredClientRepository clients,
            PasswordEncoder passwordEncoder) {
        this.clients = clients;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public RegistrationResponse register(@RequestBody RegistrationRequest request) {
        // An MCP client calls this endpoint after OAuth metadata discovery to obtain unique credentials.
        if (request.redirectUris() == null || request.redirectUris().isEmpty()) {
            throw new InvalidRegistrationException("redirect_uris is required.");
        }
        request.redirectUris().forEach(this::validateRedirectUri);

        // The plaintext secret is returned once; only its bcrypt hash is retained by the authorization server.
        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();
        String clientName = request.clientName() == null || request.clientName().isBlank()
                ? "MCP Client" : request.clientName().trim();

        // These settings define the authorization-code flow the client will use after redirecting a user.
        var client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now())
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUris(redirectUris -> redirectUris.addAll(request.redirectUris()))
                .scope("mcp.tools")
                .clientName(clientName)
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(false)
                        .build())
                .build();
        clients.save(client);

        // Respond using the standard Dynamic Client Registration field names expected by MCP OAuth libraries.
        return new RegistrationResponse(
                clientId,
                clientSecret,
                Instant.now().getEpochSecond(),
                0,
                request.redirectUris(),
                "client_secret_basic",
                List.of("authorization_code", "refresh_token"),
                List.of("code"),
                clientName,
                "mcp.tools");
    }

    private void validateRedirectUri(String value) {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRegistrationException("redirect_uris contains an invalid URL.");
        }
        // Authorization codes are sent only to HTTPS callbacks, except when developing on localhost.
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if (!(localHttp || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new InvalidRegistrationException("redirect_uris must use HTTPS, except for localhost.");
        }
    }

    public record RegistrationRequest(
            // Map OAuth DCR's snake_case JSON fields into the Java record.
            @JsonProperty("redirect_uris") List<String> redirectUris,
            @JsonProperty("grant_types") List<String> grantTypes,
            @JsonProperty("response_types") List<String> responseTypes,
            @JsonProperty("client_name") String clientName,
            @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
            String scope) {
    }

    public record RegistrationResponse(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("client_secret") String clientSecret,
            @JsonProperty("client_id_issued_at") long clientIdIssuedAt,
            @JsonProperty("client_secret_expires_at") long clientSecretExpiresAt,
            @JsonProperty("redirect_uris") List<String> redirectUris,
            @JsonProperty("token_endpoint_auth_method") String tokenEndpointAuthMethod,
            @JsonProperty("grant_types") List<String> grantTypes,
            @JsonProperty("response_types") List<String> responseTypes,
            @JsonProperty("client_name") String clientName,
            String scope) {
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(InvalidRegistrationException.class)
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRegistration(InvalidRegistrationException exception) {
        // OAuth DCR clients expect a structured protocol error rather than an HTML error page.
        return new ErrorResponse("invalid_client_metadata", exception.getMessage());
    }

    private record ErrorResponse(
            String error,
            @JsonProperty("error_description") String errorDescription) {
    }

    private static class InvalidRegistrationException extends RuntimeException {
        InvalidRegistrationException(String message) {
            super(message);
        }
    }
}
