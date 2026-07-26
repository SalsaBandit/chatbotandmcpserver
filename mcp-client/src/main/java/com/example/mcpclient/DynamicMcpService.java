package com.example.mcpclient;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springaicommunity.mcp.security.client.sync.AuthenticationMcpTransportContextProvider;
import org.springaicommunity.mcp.security.client.sync.oauth2.http.client.OAuth2DcrHttpClientTransportCustomizer;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.DynamicClientRegistrationRequest;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.McpOAuth2DcrClientManager;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicMcpService {
    // Session-scoped connections keep one browser's servers separate from another's.
    private static final String CONNECTIONS_ATTRIBUTE = DynamicMcpService.class.getName() + ".connections";

    // The OAuth callback arrives after a redirect, so this process-wide lookup reconnects it to the saved connection.
    private final Map<String, McpConnection> connectionsByRegistrationId = new ConcurrentHashMap<>();
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final McpOAuth2DcrClientManager dcrClientManager;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;

    public DynamicMcpService(
            OAuth2AuthorizedClientManager authorizedClientManager,
            ClientRegistrationRepository clientRegistrationRepository,
            McpOAuth2DcrClientManager dcrClientManager,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.dcrClientManager = dcrClientManager;
        this.authorizedClientRepository = authorizedClientRepository;
    }

    public ConnectionResponse add(
            HttpSession session,
            String name,
            String serverUrl,
            String applicationBaseUrl) {
        // Validate the browser input before it becomes an outbound OAuth or MCP URL.
        String normalizedName = requireText(name, "MCP server name");
        String normalizedUrl = validateServerUrl(serverUrl);
        String id = UUID.randomUUID().toString();
        String registrationId = "mcp-" + id;
        // Spring Security receives the provider redirect at this per-connection callback URL.
        String redirectUri = applicationBaseUrl + "/login/oauth2/code/" + registrationId;

        // Dynamic Client Registration asks the server's advertised registration endpoint for OAuth credentials.
        var registrationRequest = DynamicClientRegistrationRequest.builder()
                .grantTypes(List.of(AuthorizationGrantType.AUTHORIZATION_CODE))
                .redirectUris(List.of(redirectUri))
                .clientName("MCP Chatbot")
                .build();
        dcrClientManager.registerMcpClient(registrationId, normalizedUrl, registrationRequest);

        // Store only connection metadata here; OAuth tokens are managed by Spring Security separately.
        var connection = new McpConnection(id, normalizedName, normalizedUrl, registrationId);
        connections(session).put(id, connection);
        connectionsByRegistrationId.put(registrationId, connection);
        return toResponse(connection, false);
    }

    public void restoreConnection(HttpSession session, String registrationId) {
        // After the OAuth redirect, make sure the connection is again visible in this browser session.
        McpConnection connection = connectionsByRegistrationId.get(registrationId);
        if (connection != null) {
            connections(session).putIfAbsent(connection.id(), connection);
        }
    }

    public List<ConnectionResponse> list(HttpServletRequest request) {
        HttpSession session = request.getSession();
        // The UI receives a safe view model, including the local link that starts authorization.
        return new ArrayList<>(connections(session).values()).stream()
                .map(connection -> toResponse(connection, isAuthorized(connection, request)))
                .toList();
    }

    public List<org.springframework.ai.tool.ToolCallback> tools(HttpSession session) {
        // Each session connection becomes an MCP client; its discovered tools become model-callable callbacks.
        List<McpSyncClient> clients = connections(session).values().stream()
                .map(this::createClient)
                .toList();
        return SyncMcpToolCallbackProvider.syncToolCallbacks(clients);
    }

    private McpSyncClient createClient(McpConnection connection) {
        URI uri = URI.create(connection.serverUrl());
        // Streamable HTTP separates the origin used to create the client from its MCP endpoint path.
        String baseUrl = uri.getScheme() + "://" + uri.getAuthority();
        String endpoint = StringUtils.hasText(uri.getRawPath()) && !"/".equals(uri.getRawPath())
                ? uri.getRawPath() : "/mcp";
        var transportBuilder = HttpClientStreamableHttpTransport.builder(baseUrl).endpoint(endpoint);
        // Attach the matching OAuth client. It obtains/refreshes a token before MCP requests are sent.
        new OAuth2DcrHttpClientTransportCustomizer(
                authorizedClientManager, clientRegistrationRepository, dcrClientManager, connection.registrationId())
                .customize(connection.registrationId(), transportBuilder);

        return McpClient.sync(transportBuilder.build())
                .clientInfo(new McpSchema.Implementation("mcp-client", "1.0"))
                .requestTimeout(Duration.ofSeconds(30))
                // Propagate the current request's authentication context to the OAuth-aware transport.
                .transportContextProvider(new AuthenticationMcpTransportContextProvider())
                .build();
    }

    private boolean isAuthorized(McpConnection connection, HttpServletRequest request) {
        // Authorization is per MCP connection because every server has its own client registration and token.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(
                connection.registrationId(), authentication, request);
        if (client == null || client.getAccessToken() == null) {
            return false;
        }
        Instant expiresAt = client.getAccessToken().getExpiresAt();
        // An expired access token is still usable when a refresh token lets the manager renew it on demand.
        return expiresAt == null || expiresAt.isAfter(Instant.now()) || client.getRefreshToken() != null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, McpConnection> connections(HttpSession session) {
        // Lazily create the per-browser connection map and retain it in the servlet session.
        Object existing = session.getAttribute(CONNECTIONS_ATTRIBUTE);
        if (existing instanceof Map<?, ?>) {
            return (Map<String, McpConnection>) existing;
        }

        Map<String, McpConnection> connections = new ConcurrentHashMap<>();
        session.setAttribute(CONNECTIONS_ATTRIBUTE, connections);
        return connections;
    }

    private String validateServerUrl(String serverUrl) {
        String value = requireText(serverUrl, "MCP server URL");
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("MCP server URL must be a valid absolute URL.", exception);
        }

        if (uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("MCP server URL must be an absolute URL without query parameters.");
        }
        // Remote MCP/OAuth traffic must be encrypted; local HTTP is allowed for development.
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return uri.toString();
        }
        if ("http".equalsIgnoreCase(uri.getScheme())
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()))) {
            return uri.toString();
        }
        throw new IllegalArgumentException("MCP server URL must use HTTPS (HTTP is allowed only for localhost).");
    }

    private String requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }

    private record McpConnection(String id, String name, String serverUrl, String registrationId) {
    }

    // This is the JSON shape consumed by script.js, not the internal token-bearing connection state.
    public record ConnectionResponse(
            String id,
            String name,
            String serverUrl,
            String authorizeUrl,
            boolean authorized) {
    }

    private ConnectionResponse toResponse(McpConnection connection, boolean authorized) {
        // Keep the browser on this application while it starts the provider's OAuth authorization redirect.
        return new ConnectionResponse(
                connection.id(), connection.name(), connection.serverUrl(),
                "/mcp/servers/" + connection.id() + "/authorize",
                authorized);
    }
}
