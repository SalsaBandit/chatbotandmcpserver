package com.example.mcpserver;

import org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.util.Set;

@Configuration
public class SecurityConfig {

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            @Value("${mcp.oauth.issuer-uri}") String issuerUri) {
        // The issuer is the canonical base URL published in OAuth discovery documents and embedded in JWTs.
        return AuthorizationServerSettings.builder()
                .issuer(issuerUri)
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${mcp.oauth.user.email}") String email,
            @Value("${mcp.oauth.user.password}") String password,
            PasswordEncoder passwordEncoder) {
        // Demo credentials authenticate the resource owner during the OAuth authorization-code redirect.
        return new org.springframework.security.provisioning.InMemoryUserDetailsManager(
                User.withUsername(email)
                        .password(passwordEncoder.encode(password))
                        .authorities("ROLE_MCP_USER")
                        .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // Hash both the demo user password and dynamically generated OAuth client secrets.
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${mcp.oauth.issuer-uri}") String issuerUri) {
        // The MCP resource server verifies bearer tokens using this authorization server's published JWK set.
        var decoder = NimbusJwtDecoder.withJwkSetUri(issuerUri + "/oauth2/jwks").build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            AuthorizationServerSettings authorizationServerSettings) throws Exception {
        // This highest-priority chain owns OAuth authorize, token, JWK, and metadata endpoints.
        http.oauth2AuthorizationServer(authorizationServer -> {
            http.securityMatcher(authorizationServer.getEndpointsMatcher());
            authorizationServer
                    .authorizationServerSettings(authorizationServerSettings)
                    .authorizationServerMetadataEndpoint(metadata -> metadata
                            .authorizationServerMetadataCustomizer(builder -> builder
                                    // DCR-aware MCP clients discover where to create their OAuth client credentials.
                                    .claim("registration_endpoint", authorizationServerSettings.getIssuer() + "/register")))
                    .oidc(oidc -> oidc.providerConfigurationEndpoint(provider -> provider
                            .providerConfigurationCustomizer(builder -> builder
                                    .claim("registration_endpoint", authorizationServerSettings.getIssuer() + "/register"))));
        });
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated());
        http.exceptionHandling(exceptions -> {
            var matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
            matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
            exceptions.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), matcher);
        });
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain mcpResourceSecurityFilterChain(
            HttpSecurity http,
            @Value("${mcp.oauth.issuer-uri}") String issuerUri,
            JwtDecoder jwtDecoder) throws Exception {
        // Configure /mcp as an OAuth-protected resource, separate from this application's authorization server.
        var mcpOAuth = McpServerOAuth2Configurer.mcpServerOAuth2()
                .authorizationServer(issuerUri)
                .jwtDecoder(jwtDecoder)
                .resourcePath("/mcp")
                .resourceName("Expense MCP Server")
                .validateAudienceClaim(false)
                .protectedResourceMetadataCustomizer(metadata -> metadata
                        .authorizationServer(issuerUri)
                        .resourceName("Expense MCP Server")
                        .scope("mcp.tools"))
                // Bind the stateful OAuth browser flow to a session while the MCP protocol stays stateless.
                .sessionBinding(Customizer.withDefaults());

        http
                // Clients query protected-resource metadata before they know which authorization server to use.
                .securityMatcher("/mcp/**", "/.well-known/oauth-protected-resource/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/oauth-protected-resource/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(AbstractHttpConfigurer::disable)
                .with(mcpOAuth, Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        // Non-OAuth, non-MCP routes use the fallback chain and remain publicly reachable in this demo.
        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll())
                .formLogin(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
