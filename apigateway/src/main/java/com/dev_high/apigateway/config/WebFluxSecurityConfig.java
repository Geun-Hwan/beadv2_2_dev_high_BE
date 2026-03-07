package com.dev_high.apigateway.config;

import com.dev_high.apigateway.security.AuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {
    private final static String[] PERMITALL_ANTPATTERNS = {
            "/", "/csrf",
            "/?*-service/swagger-ui/**",
            "/?*-service/actuator/?*", "/actuator/**",
            "/v3/api-docs/**", "/?*-service/v3/api-docs", "/swagger*/**", "/webjars/**"
    };

    @Bean
    public SecurityWebFilterChain configure(
            ServerHttpSecurity http,
            ReactiveAuthorizationManager<AuthorizationContext> authorizationManager,
            AuthenticationFilter authenticationFilter
    ) {
        http
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .headers(headers -> headers.frameOptions(
                        ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable))
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOriginPatterns(List.of(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "https://more-auction.kro.kr",
                            "https://more-admin.kro.kr"
                    ));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(PERMITALL_ANTPATTERNS).permitAll()
                        .anyExchange().access(authorizationManager)
                );

        return http.build();
    }
}
