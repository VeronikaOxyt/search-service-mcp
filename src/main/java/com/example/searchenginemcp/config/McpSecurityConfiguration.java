package com.example.searchenginemcp.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(
        name = "mcp.security.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class McpSecurityConfiguration {

    @Bean
    SecurityFilterChain mcpSecurityFilterChain(
            HttpSecurity http,
            McpSecurityProperties properties) throws Exception {
        validate(properties);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/**", "/actuator/health", "/error")
                        .permitAll()
                        .requestMatchers("/mcp")
                        .authenticated()
                        .anyRequest()
                        .denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(
                                new McpBearerAuthenticationEntryPoint(properties)))
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(McpSecurityProperties properties) {
        validate(properties);
        return new SupplierJwtDecoder(() -> createJwtDecoder(properties));
    }

    static JwtDecoder createJwtDecoder(McpSecurityProperties properties) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(
                properties.getIssuerUri());
        var issuerValidator = JwtValidators.createDefaultWithIssuer(properties.getIssuerUri());
        var audienceValidator = new JwtClaimValidator<List<String>>(
                "aud",
                audiences -> audiences != null && audiences.contains(properties.getAudience()));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
                issuerValidator,
                audienceValidator));
        return decoder;
    }

    private static void validate(McpSecurityProperties properties) {
        if (properties.getIssuerUri() == null || properties.getIssuerUri().isBlank()) {
            throw new IllegalStateException(
                    "MCP_AUTH_ISSUER_URI must be set when MCP security is enabled");
        }
        if (properties.getAudience() == null || properties.getAudience().isBlank()) {
            throw new IllegalStateException(
                    "MCP_AUTH_AUDIENCE must be set when MCP security is enabled");
        }
        if (properties.getResourceUri() == null || properties.getResourceUri().isBlank()) {
            throw new IllegalStateException(
                    "MCP_RESOURCE_URI must be set when MCP security is enabled");
        }
        if (properties.getMetadataUri() == null || properties.getMetadataUri().isBlank()) {
            throw new IllegalStateException(
                    "MCP_RESOURCE_METADATA_URI must be set when MCP security is enabled");
        }
    }
}
