package com.dev_high.apigateway.service;

import io.r2dbc.postgresql.api.Notification;
import io.r2dbc.postgresql.api.PostgresqlConnection;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class EndpointRuleChangeListener {

    private static final Duration RELOAD_DEBOUNCE = Duration.ofMillis(300);

    private final ConnectionFactory connectionFactory;
    private final EndpointRuleCache cache;

    private final Sinks.Many<String> reloadSignals =
            Sinks.many().multicast().onBackpressureBuffer();

    private final AtomicBoolean reloadRunning = new AtomicBoolean(false);
    private final AtomicBoolean reloadPending = new AtomicBoolean(false);
    @Value("${app.gateway.auth-bypass:false}")
    private boolean authBypass;

    private Disposable reloadWorker;
    private Disposable listenWorker;

    public EndpointRuleChangeListener(
            @Qualifier("listenConnectionFactory") ConnectionFactory connectionFactory,
            EndpointRuleCache cache
    ) {
        this.connectionFactory = connectionFactory;
        this.cache = cache;
    }

    // 룰 변경 이벤트 발생 신호 발행
    public void requestReload(String reason) {
        var res = reloadSignals.tryEmitNext(reason);
        if (res.isFailure()) {
            log.warn("Rule 캐시 리로드 요청 신호 전송 실패 - 사유: {}, 결과: {}", reason, res);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startOnReady() {
        if (authBypass) {
            log.info("auth-bypass 활성화로 Rule 변경 LISTEN 워커를 비활성화합니다.");
            return;
        }
        reloadWorker = reloadSignals.asFlux()
                .publishOn(Schedulers.boundedElastic())
                .concatMap(this::triggerReload)
                .onErrorResume(e -> {
                    log.error("Rule 리로드 파이프라인 오류", e);
                    return Mono.empty();
                })
                .subscribe();

        listenWorker = startListenLoop()
                .doOnSubscribe(s -> log.warn("LISTEN 루프 시작"))
                .doOnError(e -> log.error("LISTEN 루프 오류", e))
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(rs ->
                                log.warn("LISTEN 재시도 - 원인: {}", rs.failure().toString())))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private Mono<Void> triggerReload(String sig) {
        if (reloadRunning.get()) {
            reloadPending.set(true);
            return Mono.empty();
        }

        if (!reloadRunning.compareAndSet(false, true)) {
            reloadPending.set(true);
            return Mono.empty();
        }

        return Mono.delay(RELOAD_DEBOUNCE)
                .then(cache.reload()
                        .doOnSubscribe(s -> log.info("Rule 캐시 리로드 시작 (신호={})", sig))
                        .doOnSuccess(v -> log.info("Rule 캐시 리로드 완료 (신호={})", sig))
                        .onErrorResume(e -> {
                            log.error("Rule 캐시 리로드 실패 (신호={})", sig, e);
                            return Mono.empty();
                        }))
                .doFinally(endSig -> {
                    reloadRunning.set(false);

                    if (reloadPending.compareAndSet(true, false)) {
                        requestReload("pending");
                    }
                });
    }

    private Mono<Void> startListenLoop() {
        return Mono.usingWhen(
                Mono.from(connectionFactory.create()),
                this::listen,
                Connection::close
        );
    }

    private Mono<Void> listen(Connection conn) {
        if (!(conn instanceof PostgresqlConnection pg)) {
            return Mono.error(new IllegalStateException("PostgreSQL 전용 LISTEN 연결이 아닙니다"));
        }


        Mono<Void> listenReady =
                Flux.from(pg.createStatement("LISTEN endpoint_rule_changed").execute())
                        .flatMap(result -> result.map((row, meta) -> 1)) // consume
                        .then();

        return listenReady
                .thenMany(pg.getNotifications()
                        .doOnNext(this::onNotification)
                )
                .then();
    }

    private void onNotification(Notification n) {
        log.info("Rule 변경 NOTIFY 수신 - channel={}", n.getName());
        requestReload("notify");
    }

    @PreDestroy
    public void stop() {
        if (listenWorker != null) {
            listenWorker.dispose();
        }
        if (reloadWorker != null) {
            reloadWorker.dispose();
        }
    }
}
