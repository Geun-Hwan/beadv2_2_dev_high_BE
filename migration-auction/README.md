# migration-auction 실행 가이드

## 1) JDK만 있으면 되나?
- 결론: JDK만으로는 부족합니다.
- Kotlin 컴파일러/빌드 도구가 필요하며, 이 프로젝트는 루트의 Gradle Wrapper(`./gradlew`)를 사용합니다.

## 2) 필수 준비물
- JDK 17 (`java -version` 확인)
- 네트워크 (최초 1회 의존성 다운로드)

## 3) 실행 절차
1. Kotlin 컴파일 확인
```bash
./gradlew -p migration-auction compileKotlin
```
2. 애플리케이션 실행
```bash
./gradlew -p migration-auction bootRun
```
3. 헬스 체크
```bash
curl http://localhost:18081/migration/ping
```

## 4) 현재 구조 원칙
- 실제 마이그레이션 소스는 `src/main/kotlin`만 사용
- `auction` 기존 모듈은 수정하지 않음
- 기존 Java 패키지 구조와 최대한 동일하게 유지
