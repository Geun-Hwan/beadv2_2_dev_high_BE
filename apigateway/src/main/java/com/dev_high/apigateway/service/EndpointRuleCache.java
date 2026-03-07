package com.dev_high.apigateway.service;

import com.dev_high.apigateway.repository.EndpointPolicyRepository;
import com.dev_high.apigateway.repository.dto.EndpointRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndpointRuleCache {

    private final EndpointPolicyRepository repository;
    private final AtomicReference<Map<String, List<EndpointRule>>> cacheRef = new AtomicReference<>(Map.of());
    private final PathPatternParser parser = new PathPatternParser();
    private final AtomicLong lastLoadedVersion = new AtomicLong(-1);
    @Value("${app.gateway.auth-bypass:false}")
    private boolean authBypass;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (authBypass) {
            lastLoadedVersion.set(0);
            log.info("auth-bypass 활성화로 Rule 캐시 로딩을 건너뜁니다.");
            return;
        }
        reload()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Rule 캐시 초기 로딩 실패", e))
                .subscribe();
    }

    public Mono<Void> reload() {
        if (authBypass) {
            return Mono.empty();
        }
        return repository.getRuleVersion()
                .defaultIfEmpty(0L)
                .flatMap(ver -> {
                    if (ver == lastLoadedVersion.get()) {
                        return Mono.empty();
                    }

                    return repository.loadAllPolicies()
                            .handle((row, sink) -> {
                                try {
                                    PathPattern pattern = parser.parse(row.path());
                                    sink.next(new RuleWithMethod(
                                            row.method(),
                                            new EndpointRule(pattern, row.authRequired(), row.roles())
                                    ));
                                } catch (Exception e) {
                                    log.warn("Endpoint path 패턴 파싱 실패 - skip: path={}, method={}, err={}",
                                            row.path(), row.method(), e.getMessage());
                                }
                            })
                            .cast(RuleWithMethod.class)
                            .collectList()
                            .doOnNext(list -> {
                                Map<String, List<EndpointRule>> grouped = list.stream()
                                        .collect(Collectors.groupingBy(
                                                RuleWithMethod::method,
                                                Collectors.mapping(RuleWithMethod::rule, Collectors.toList())
                                        ));

                                Map<String, List<EndpointRule>> newCache = new HashMap<>();
                                for (var e : grouped.entrySet()) {
                                    newCache.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
                                }

                                cacheRef.set(Collections.unmodifiableMap(newCache));
                                lastLoadedVersion.set(ver);

                                log.info("Rule 캐시 로딩 완료 - 규칙 수: {}, 메서드: {}, version={}",
                                        list.size(), newCache.keySet(), ver);
                            })
                            .then();
                });
    }

    public List<EndpointRule> getCandidates(String method) {
        Map<String, List<EndpointRule>> cache = cacheRef.get();

        List<EndpointRule> exact = cache.getOrDefault(method, Collections.emptyList());
        List<EndpointRule> any = cache.getOrDefault("*", Collections.emptyList());

        if (exact.isEmpty()) return any;
        if (any.isEmpty()) return exact;

        return Stream.concat(exact.stream(), any.stream()).toList();
    }

    public long getLastLoadedVersion() {
        return lastLoadedVersion.get();
    }

    private record RuleWithMethod(String method, EndpointRule rule) {}
}
