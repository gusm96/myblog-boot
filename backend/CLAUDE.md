# Backend CLAUDE.md

## 기술 스택 및 버전

### 언어 / 런타임

| 항목 | 버전 |
|------|------|
| Java | 17 (sourceCompatibility) |
| JDK (Docker) | Eclipse Temurin 17 (alpine) |

### 프레임워크 / 핵심 라이브러리

| 항목 | 버전 | 비고 |
|------|------|------|
| Spring Boot | 3.0.4 | |
| Spring Dependency Management | 1.1.0 | |
| Spring Security | (Boot 관리) | |
| Spring Data JPA | (Boot 관리) | Hibernate, MariaDBDialect |
| Spring Data Redis | (Boot 관리) | |
| Spring Cloud AWS | 2.2.6.RELEASE | S3 파일 업로드 |
| Lettuce | 6.2.6.RELEASE | Redis 클라이언트 |
| QueryDSL | 5.0.0 (jakarta) | |
| jjwt (io.jsonwebtoken) | 0.12.6 | api / impl / jackson |
| Jackson | 2.17.2 | dependencyManagement 강제 오버라이드 |
| Lombok | (Boot 관리) | |
| H2 | (Boot 관리) | 테스트/내장 DB |

### 데이터베이스 / 인프라

| 항목 | 버전 | 비고 |
|------|------|------|
| MariaDB | 10.11 | Docker 이미지 기준 |
| Redis | 7-alpine | AOF 활성화, Docker |

### 빌드 / 문서

| 항목 | 버전 |
|------|------|
| Gradle | Wrapper 사용 |
| Asciidoctor | 3.3.2 (jvm.convert) |
| Spring REST Docs | (Boot 관리, mockmvc) |

### 테스트

| 항목 | 버전 |
|------|------|
| JUnit 5 | (Boot 관리) |
| Testcontainers | 2.0.3 |
| Spring Security Test | (Boot 관리) |

### Docker

- **빌드 이미지**: `eclipse-temurin:17-jdk-alpine`
- **실행 이미지**: `eclipse-temurin:17-jre-alpine`
- JVM 옵션: `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`

---

## 빌드 & 실행 명령어

```bash
# 빌드 (테스트 포함)
./gradlew build

# 테스트만
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.moya.myblogboot.controller.BoardControllerTest"

# bootJar (문서 생성 포함)
./gradlew bootJar

# Docker
docker compose --env-file backend/.env.prod up -d
```
