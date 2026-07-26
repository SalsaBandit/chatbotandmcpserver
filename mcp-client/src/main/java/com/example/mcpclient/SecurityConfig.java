package com.example.mcpclient;

import org.springaicommunity.mcp.security.client.sync.config.McpClientOAuth2Configurer;
import org.springaicommunity.mcp.security.client.sync.oauth2.metadata.McpMetadataDiscoveryService;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.DefaultMcpOAuth2DcrClientManager;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.DynamicClientRegistrationService;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.InMemoryMcpClientRegistrationRepository;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.McpClientRegistrationRepository;
import org.springaicommunity.mcp.security.client.sync.oauth2.registration.McpOAuth2DcrClientManager;
import org.springaicommunity.mcp.security.common.url.DefaultUrlValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // This demo authorizes individual MCP connections rather than requiring an application login.
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                // Adds OAuth discovery, DCR, and callback support for MCP servers added at runtime.
                .with(new McpClientOAuth2Configurer(), Customizer.withDefaults());

        return http.build();
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        // Dynamic registrations exist for the application lifetime and are keyed by mcp-{connection-id}.
        return new InMemoryMcpClientRegistrationRepository();
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository) {
        var manager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository);
        // Authorization-code obtains the first token; refresh-token keeps later MCP calls authenticated.
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return manager;
    }

    @Bean
    McpOAuth2DcrClientManager mcpOAuth2DcrClientManager(ClientRegistrationRepository clientRegistrationRepository) {
        // The manager discovers OAuth metadata from an MCP URL and registers this client with that server.
        var urlValidator = new DefaultUrlValidator(true);
        return new DefaultMcpOAuth2DcrClientManager(
                (McpClientRegistrationRepository) clientRegistrationRepository,
                new DynamicClientRegistrationService(urlValidator),
                new McpMetadataDiscoveryService(urlValidator),
                urlValidator);
    }
}
