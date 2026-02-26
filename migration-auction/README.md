# migration-auction 실행 가이드

## prerequisites
- JDK 17
- root gradle wrapper (`./gradlew`)
- 최초 1회 의존성 다운로드를 위한 네트워크
- `migration-auction/settings.gradle` 기반 단독 실행

## run
```bash
./gradlew -p migration-auction compileKotlin
./gradlew -p migration-auction bootRun
```

## check
```bash
curl http://localhost:18081/migration/ping
```

## rule
- 기존 `auction` 모듈은 수정하지 않음
- 마이그레이션 코드는 `migration-auction/src/main/kotlin`에서만 관리
- 패키지 구조는 `auction` 기준을 최대한 유지
