package com.dev_high.apigateway.security;

import com.dev_high.apigateway.jwt.JwtClaimUtils;
import com.dev_high.apigateway.jwt.JwtProvider;
import com.dev_high.apigateway.repository.dto.EndpointRule;
import com.dev_high.apigateway.service.EndpointRuleCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.pattern.PathPattern;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements WebFilter {

    public static final String ENDPOINT_RULE = "ENDPOINT_RULE";
    private static final String HEADER_USER_ID = "X-User-Id";

    private final JwtProvider jwtProvider;
    private final EndpointRuleCache ruleCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        var request = exchange.getRequest();

        HttpMethod method = request.getMethod();
        String path = request.getPath().value();

        if (method == HttpMethod.OPTIONS || isWellKnownPublicPath(path)) {
            return chain.filter(exchange);
        }

        EndpointRule target = selectMostSpecific(ruleCache.getCandidates(method.name()), path);
        if (target == null) {
            return unauthorized(exchange,HttpStatus.FORBIDDEN);

        }
        exchange.getAttributes().put(ENDPOINT_RULE, target);
        log.info("target : {}",target);
        if (!target.authRequired()) {
            return propagateUserContextIfTokenPresent(exchange, chain);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange,HttpStatus.UNAUTHORIZED);
        }

        String token = authHeader.substring(7).trim();

        Claims claims;
        try {
            claims = jwtProvider.parseToken(token);
        } catch (ExpiredJwtException e) {
            log.warn("JWT 만료: {}", e.getMessage());
            return unauthorized(exchange,HttpStatus.UNAUTHORIZED);

        } catch (Exception e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return unauthorized(exchange,HttpStatus.UNAUTHORIZED);
        }

        String userId = claims.getSubject();
        Set<String> roles = JwtClaimUtils.extractRoles(claims);

        var authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(
                userId,
                token,
                authorities
        );

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(h -> h.remove(HEADER_USER_ID))
                .header(HEADER_USER_ID, userId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        return chain.filter(mutatedExchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    private Mono<Void> propagateUserContextIfTokenPresent(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7).trim();
        try {
            Claims claims = jwtProvider.parseToken(token);
            String userId = claims.getSubject();
            if (userId == null || userId.isBlank()) {
                return chain.filter(exchange);
            }

            Set<String> roles = JwtClaimUtils.extractRoles(claims);
            var authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
            var authentication = new UsernamePasswordAuthenticationToken(userId, token, authorities);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(h -> h.remove(HEADER_USER_ID))
                    .header(HEADER_USER_ID, userId)
                    .build();
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            return chain.filter(mutatedExchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        } catch (Exception e) {
            // auth_required=false 경로에서는 유효하지 않은 토큰이어도 익명 요청으로 통과.
            return chain.filter(exchange);
        }
    }

    private EndpointRule selectMostSpecific(List<EndpointRule> candidates, String requestPath) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        PathContainer pathContainer = PathContainer.parsePath(requestPath);

        return candidates.stream()
                .filter(r -> r.pattern().matches(pathContainer))
                .min((a, b) -> PathPattern.SPECIFICITY_COMPARATOR.compare(a.pattern(), b.pattern()))
                .orElse(null);
    }

    private boolean isWellKnownPublicPath(String path) {

        return path.equals("/") ||
                path.startsWith("/actuator") ||
                path.startsWith("/v3/api-docs") ||
                path.contains("/swagger-ui") ||
                path.startsWith("/swagger") ||
                path.startsWith("/webjars");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange ,HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
}
