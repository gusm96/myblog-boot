# S3 버킷 삭제 후 개발 환경 오류 해결 계획

## 1. 문제 상황

AWS S3 버킷을 비용 문제로 삭제하였으나, 코드에서 여전히 S3를 참조하고 있어
개발 환경 애플리케이션 실행 시 오류가 발생한다.

---

## 2. 원인 분석

### 2-1. 의존성 구조

```
spring-cloud-starter-aws:2.2.6.RELEASE (build.gradle)
  └─ ContextCredentialsAutoConfiguration (Spring Cloud AWS 자동 구성)
       └─ cloud.aws.credentials.access-key / secret-key 프로퍼티 참조

AwsS3Config.java
  └─ @Value("${cloud.aws.credentials.access-key}") → AWS_CREDENTIALS_ACCESS_KEY 환경변수 필요
  └─ @Value("${cloud.aws.credentials.secret-key}") → AWS_CREDENTIALS_SECRET_KEY 환경변수 필요
  └─ AmazonS3 빈 생성

FileUploadServiceImpl.java
  └─ AmazonS3 의존성 주입 → 실제 S3 API 호출
```

### 2-2. 실행 시 오류 흐름

```
애플리케이션 시작
  → application.yaml 로드
      cloud.aws.credentials.access-key: ${AWS_CREDENTIALS_ACCESS_KEY}  ← 기본값 없음
  → AwsS3Config 빈 생성 시도
      @Value("${AWS_CREDENTIALS_ACCESS_KEY}") 주입 실패
  → BeanCreationException: Could not resolve placeholder 'AWS_CREDENTIALS_ACCESS_KEY'
  → 애플리케이션 기동 실패
```

### 2-3. 문제가 되는 파일 목록

| 파일 | 문제 내용 |
|------|-----------|
| `src/main/resources/application.yaml` | `${AWS_CREDENTIALS_ACCESS_KEY}`, `${AWS_CREDENTIALS_SECRET_KEY}` — 기본값 없음 |
| `src/main/java/.../configuration/AwsS3Config.java` | `@Value`로 필수 환경변수 참조 → 개발 환경에서 빈 생성 실패 |
| `src/main/java/.../service/implementation/FileUploadServiceImpl.java` | `AmazonS3` 빈 의존 → prod/dev 구분 없이 항상 S3 호출 시도 |

---

## 3. 해결 방안 비교

### 방안 A — Spring Profile 기반 구현체 분리 (권장)

- dev 프로파일: S3 없이 동작하는 로컬 no-op 구현체 사용
- prod 프로파일: 기존 S3 구현체 유지
- `application-dev.yaml`에 더미 자격증명 추가 → Spring Cloud AWS 자동 구성 placeholder 오류 해소

**장점**: 코드 변경이 명확하고 프로파일별 책임이 분리됨
**단점**: 새 구현체 파일 추가 필요

### 방안 B — `@ConditionalOnProperty` 조건부 빈 등록

- `cloud.aws.enabled: true/false` 플래그로 S3 빈 등록 여부 결정
- 플래그가 false이면 LocalFileUploadServiceImpl 사용

**장점**: 프로파일과 독립적으로 on/off 제어 가능
**단점**: 방안 A보다 설정이 복잡하고 기존 환경변수 문제는 별도 해소 필요

### 방안 C — 환경변수에 더미값 설정 (임시 처방)

- `.env` 파일 또는 IDE 실행 설정에 `AWS_CREDENTIALS_ACCESS_KEY=dummy` 추가

**장점**: 코드 변경 없음
**단점**: 팀원마다 설정해야 함, 근본적 해결 아님, 실수로 실제 S3에 접근할 위험

---

## 4. 권장 해결 방안 상세 (방안 A)

### 4-1. 변경 대상 파일

```
수정
  src/main/resources/application-dev.yaml
  src/main/java/.../configuration/AwsS3Config.java
  src/main/java/.../service/implementation/FileUploadServiceImpl.java

신규 생성
  src/main/java/.../service/implementation/LocalFileUploadServiceImpl.java
```

### 4-2. `application-dev.yaml` 수정

`application.yaml`의 `${AWS_CREDENTIALS_ACCESS_KEY}` placeholder를 dev 프로파일에서
더미 리터럴 값으로 덮어써서 Spring Cloud AWS 자동 구성 오류를 방지한다.

```yaml
# 기존 내용 유지 ...

# S3 (개발 환경: 실제 버킷 없음, 더미 값으로 placeholder 해소)
cloud:
  aws:
    credentials:
      access-key: dev-dummy-access-key
      secret-key: dev-dummy-secret-key
    s3:
      bucketName: dev-dummy-bucket
    region:
      static: ap-northeast-2
      auto: false
    stack:
      auto: false   # Spring Cloud AWS Stack 자동 감지 비활성화
```

> **왜 더미값이 필요한가?**
> `application.yaml`에 `${AWS_CREDENTIALS_ACCESS_KEY}` 가 선언되어 있으면,
> `AwsS3Config` 빈 생성 여부와 무관하게 **Spring Cloud AWS 자동 구성**
> (`ContextCredentialsAutoConfiguration`)이 해당 프로퍼티를 읽으려 시도한다.
> dev 프로파일의 `application-dev.yaml`이 base `application.yaml`보다 우선 적용되므로,
> 리터럴 더미값으로 덮어쓰면 placeholder 해석 오류가 발생하지 않는다.

### 4-3. `AwsS3Config.java` 수정

```java
@Configuration
@Profile("prod")   // ← 추가: prod 프로파일에서만 AmazonS3 빈 생성
public class AwsS3Config {
    // 기존 코드 동일
}
```

prod 프로파일에서만 커스텀 `AmazonS3` 빈을 등록한다.
dev 프로파일에서는 이 빈이 생성되지 않으므로 S3 자격증명 주입 자체가 발생하지 않는다.

### 4-4. `FileUploadServiceImpl.java` 수정

```java
@Slf4j
@Service
@Profile("prod")   // ← 추가: prod 프로파일에서만 S3 구현체 사용
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {
    // 기존 코드 동일
}
```

### 4-5. `LocalFileUploadServiceImpl.java` 신규 생성

dev 프로파일 전용 no-op 구현체. 실제 파일 저장 없이 로그만 출력하고 더미 DTO를 반환한다.

```java
package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.file.ImageFile;
import com.moya.myblogboot.domain.file.ImageFileDto;
import com.moya.myblogboot.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Profile("dev")
public class LocalFileUploadServiceImpl implements FileUploadService {

    @Override
    public ImageFileDto upload(MultipartFile file) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        log.info("[DEV] 파일 업로드 시뮬레이션: {}", fileName);
        return ImageFileDto.builder()
                .fileName(fileName)
                .filePath("http://localhost:8080/dev-images/" + fileName)
                .build();
    }

    @Override
    public void delete(String imageFileName) {
        log.info("[DEV] 파일 삭제 시뮬레이션: {}", imageFileName);
    }

    @Override
    public void deleteFiles(List<ImageFile> imageFiles) {
        log.info("[DEV] 파일 삭제 시뮬레이션: {}개 파일", imageFiles.size());
    }
}
```

---

## 5. 적용 후 동작 구조

```
dev 프로파일 실행 시
  application.yaml (base) + application-dev.yaml (override) 로드
    cloud.aws.credentials.access-key = "dev-dummy-access-key"  ← 더미값으로 오버라이드
    cloud.aws.stack.auto = false
  Spring Cloud AWS 자동 구성 → 더미 AmazonS3 빈 생성 (사용되지 않음)
  AwsS3Config (@Profile("prod")) → 생략
  LocalFileUploadServiceImpl (@Profile("dev")) → 활성화
  FileUploadServiceImpl (@Profile("prod")) → 생략
  → 앱 기동 성공, 파일 업로드 API는 로그 출력 후 더미 응답 반환

prod 프로파일 실행 시 (변경 없음)
  AWS_CREDENTIALS_ACCESS_KEY, AWS_CREDENTIALS_SECRET_KEY 환경변수 필수
  AwsS3Config (@Profile("prod")) → AmazonS3 빈 생성
  FileUploadServiceImpl (@Profile("prod")) → S3 실제 업로드/삭제
```

---

## 6. 체크리스트

- [ ] `application-dev.yaml` — cloud.aws 더미값 추가, `stack.auto: false` 추가
- [ ] `AwsS3Config.java` — `@Profile("prod")` 추가
- [ ] `FileUploadServiceImpl.java` — `@Profile("prod")` 추가
- [ ] `LocalFileUploadServiceImpl.java` — 신규 생성
- [ ] dev 프로파일로 앱 실행하여 기동 성공 확인
- [ ] 파일 업로드 API(`POST /api/v1/images`) 호출 시 더미 응답 반환 확인
- [ ] prod 프로파일 빌드 시 기존 동작에 영향 없음 확인 (CI에서 검증)

---

## 7. 향후 고려 사항

| 항목 | 설명 |
|------|------|
| **MinIO 도입** | 로컬에서 S3 호환 오브젝트 스토리지를 실행하여 실제 파일 업로드/조회까지 테스트 가능. Docker Compose에 MinIO 서비스 추가 필요. |
| **S3 재도입 시** | `AwsS3Config`와 `FileUploadServiceImpl`에서 `@Profile("prod")` 제거 후 원복. `application-dev.yaml`의 더미값 섹션 제거. |
| **테스트 환경** | `application-test.yaml`은 이미 더미값이 설정되어 있어 변경 불필요. |
