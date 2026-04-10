# 1단계: 빌드
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 의존성 캐시 레이어 분리 (소스 변경 시 재다운로드 방지)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 빌드 (테스트·문서 생성 스킵 — CI에서 별도 수행)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test -x asciidoctor -x createDocument

# 2단계: 실행 이미지 (JRE만 포함 — 경량화)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
