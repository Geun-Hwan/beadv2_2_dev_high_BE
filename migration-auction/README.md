# migration-auction 실행 가이드

## prerequisites
- JDK 17
- root gradle wrapper (`./gradlew`)
- 최초 1회 의존성 다운로드를 위한 네트워크
- `migration-auction/settings.gradle` 기반 단독 실행

## status
- `auction/src/main/java/com/dev_high` 기준 Kotlin 마이그레이션 완료
- `migration-auction` 단독 컴파일/테스트/패키징 가능

## run
```bash
./gradlew -p migration-auction compileKotlin
./gradlew -p migration-auction test
./gradlew -p migration-auction build
./gradlew -p migration-auction bootRun
# 포트 충돌 시
./gradlew -p migration-auction bootRun --args='--server.port=18084'
```

## check
```bash
curl http://localhost:18081/migration/ping
```

## standalone defaults
- `spring.batch.job.enabled=false`
- `spring.kafka.listener.auto-startup=false`
- `spring.data.redis.repositories.enabled=false`
- DB는 H2(in-memory) 기본 사용

## rule
- 기존 `auction` 모듈은 수정하지 않음
- 마이그레이션 코드는 `migration-auction/src/main/kotlin`에서만 관리
- 패키지 구조는 `auction` 기준을 최대한 유지
