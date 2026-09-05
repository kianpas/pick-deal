# 06. 배포 설계 (Deployment)

> PickDeal의 현재 로컬 환경과 최초 상시 배포 계약을 관리한다. 작업 순서와 상태는 `docs/roadmap.md`에서만 관리한다.
> 최초 작성: 2026-05-20 · 현재 상태 갱신: 2026-09-05

## 1. 기본 배포 구성

```text
사용자
  │ HTTPS
  ▼
Vercel
└─ Next.js frontend
       │ HTTPS REST
       ▼
OCI Compute
└─ reverse proxy → Spring Boot backend + scheduler
                         │ JDBC/TLS
                         ▼
                    PostgreSQL
```

- frontend는 Vercel, backend는 OCI Compute 한 대를 기본으로 한다.
- backend와 scheduler는 같은 Spring Boot 프로세스에서 실행한다. 운영 인스턴스는 하나만 둬 중복 수집을 막는다.
- frontend까지 OCI에 둘 수 있지만, 현재는 Vercel의 Next.js 빌드·배포·프리뷰 관리를 사용해 OCI 운영 범위를 줄인다.
- Redis, 별도 collector worker, 메시지 큐는 사용하지 않는다.
- 공개 backend에는 HTTPS가 필수다. reverse proxy 제품과 도메인은 구현 시 선택한다.

## 2. PostgreSQL 선택

최초 배포 전에 다음 중 하나를 선택한다. 애플리케이션의 JPA 모델과 Flyway 스키마는 어느 쪽이든 동일하게 유지한다.

### OCI 같은 서버의 PostgreSQL 컨테이너

- backend와 DB를 별도 컨테이너로 실행한다.
- DB는 Compose 내부 네트워크에서만 접근하고 `5432`를 공개하지 않는다.
- 데이터 디렉터리는 컨테이너 밖의 영속 볼륨에 연결한다.
- `pg_dump`와 볼륨 백업·복구 절차를 직접 관리한다.

추가 서비스 비용과 네트워크 지연은 작지만, 서버 장애와 DB 운영 책임이 한 서버에 모인다.

### Supabase PostgreSQL

- OCI에는 backend 컨테이너만 두고 JDBC/TLS로 Supabase에 연결한다.
- 지속 실행 backend는 direct connection을 우선하고, 네트워크가 IPv4만 지원하면 shared pooler의 session mode를 사용한다.
- Free 플랜을 쓰면 용량·egress·비활성 정지와 자동 백업 부재를 확인하고 수동 백업을 둔다.
- 운영 데이터가 중요해지면 비활성 정지와 자동 백업 정책을 기준으로 유료 전환을 판단한다.

DB 위치는 아직 확정하지 않았다. 비용을 최소화하고 DB 운영도 경험하려면 OCI 컨테이너, backend 운영에 집중하려면 Supabase가 적합하다.

## 3. 현재 로컬 환경

- backend는 로컬 PostgreSQL `pickdeal` DB를 사용한다.
- 단일 `application.yml`과 JPA `ddl-auto: update`로 기동한다.
- `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`로 로컬 접속 정보를 바꿀 수 있다.
- frontend는 `next dev`로 실행하고 `NEXT_PUBLIC_API_BASE_URL`로 backend 주소를 받는다.
- 테스트는 H2 in-memory와 `ddl-auto: create-drop`을 사용하며 수집 scheduler를 끈다.
- `SeedDataInitializer`는 프로필 구분 없이 빈 DB에 샘플 데이터를 넣는다.
- Dockerfile, Compose, Flyway, reverse proxy와 CI/CD는 아직 없다.

## 4. 운영 설정 계약

배포 구현에서 다음 설정을 환경변수로 주입한다. 실제 이름은 코드와 함께 확정하며 비밀값은 저장소에 커밋하지 않는다.

| 영역 | 설정 | 목적 |
| --- | --- | --- |
| frontend | backend API base URL | 브라우저와 SSR 요청 대상 |
| backend | active profile | local/prod 설정 분리 |
| backend | JDBC URL·계정·비밀번호 | PostgreSQL 연결 |
| backend | 허용 frontend origin | 실제 Vercel·운영 도메인 CORS |
| backend | collector enabled·출처별 limit | 수집 활성화와 요청 상한 |
| 접근 제어 | 배포 방식에 따른 인증값 | 개인 설정·내부 쓰기 API 보호 |

운영 프로필은 다음 조건을 만족한다.

- 샘플 Deal과 키워드 Seed를 실행하지 않는다.
- Flyway가 기준 스키마를 적용하고 JPA는 `ddl-auto: validate`를 사용한다.
- SQL debug 로그를 끈다.
- 기본 DB 비밀번호에 의존하지 않는다.
- 수집기 로그에서 출처, 성공·실패, 신규 저장 수와 필드 확보율을 확인할 수 있다.

## 5. 접근 범위와 네트워크

현재는 인증 없이 고정 `user_id = 1`을 사용한다. 다음 API를 공개하면 다른 방문자가 동일한 개인 설정이나 데이터를 변경할 수 있다.

- 출처 표시/숨김
- 키워드 등록·삭제
- 내부 Deal 등록

`/internal`이라는 경로명은 보안 경계가 아니다. 최초 배포에서는 서비스 전체를 개인 접근으로 제한하거나, 공개 조회와 쓰기 API를 구분해 보호한다. 클라이언트 번들에 비밀 헤더를 넣는 방식은 사용하지 않는다.

OCI와 호스트 방화벽에서는 필요한 공개 포트만 연다.

- `80/443`: reverse proxy
- `22`: 관리용 SSH, 가능한 한 접근 IP 제한
- backend 내부 포트와 PostgreSQL `5432`: 공개하지 않음

## 6. 배포와 검증

### 로컬 배포 형태 확인

1. 운영과 같은 이미지·환경변수로 backend를 기동한다.
2. Flyway 적용과 JPA 검증을 확인한다.
3. 재기동 후 데이터 유지 여부를 확인한다.
4. health check와 API를 호출한다.

### OCI 최초 배포

1. 네트워크와 HTTPS 진입점을 구성한다.
2. backend 이미지와 운영 환경변수를 배포한다.
3. 선택한 PostgreSQL 연결과 백업을 확인한다.
4. Vercel production·preview 환경에 맞는 API 주소와 CORS를 설정한다.
5. 브라우저에서 목록·상세·키워드·출처 설정을 확인한다.
6. OCI 네트워크에서 출처별 수집을 한 번 실행해 파싱·upsert와 요청 상한을 확인한다.

SSR 성공만으로 통합 검증을 끝내지 않는다. 출처 토글과 더 보기처럼 브라우저가 직접 수행하는 요청도 실제 도메인에서 확인한다.

## 7. 백업과 복구

- OCI DB 컨테이너: 정기 `pg_dump`, 영속 볼륨 백업, 복구 명령 확인
- Supabase Free: 수동 논리 백업과 복구 확인
- 자동 백업을 제공하는 관리형 플랜: 보존 기간과 실제 복구 절차 확인

백업 생성 성공만 보지 않고 빈 테스트 DB에 복구할 수 있는지 한 번 확인한다.

## 8. 지금 만들지 않는 것

- Kubernetes와 다중 인스턴스
- Redis와 별도 worker
- 무중단 배포·고가용성 구성
- 본격적인 모니터링 스택
- 복잡한 CI/CD와 이미지 레지스트리 자동화

운영에서 반복 배포나 장애 대응 비용이 확인되면 필요한 항목부터 추가한다.

## 9. 관련 문서

- 다음 작업과 완료 조건: `docs/roadmap.md`
- DB 스키마와 Flyway 방향: `docs/04-database-design.md`
- 수집 설정과 실수집 확인: `docs/05-collector-design.md`
- 검토 근거: `docs/notes/2026-09-05-pickdeal-review.md`
