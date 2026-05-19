# Pick Deal 배포 설계 초안

## 1. 배포 목표

초기 배포는 개인용 서비스 운영을 기준으로 단순하고 유지보수하기 쉬운 구조를 선택한다.

우선순위:

1. 배포와 복구가 단순해야 한다.
2. 프론트엔드, 백엔드, DB가 명확히 분리되어야 한다.
3. 추후 수집기와 AI worker를 별도로 추가할 수 있어야 한다.
4. 비용을 낮게 유지해야 한다.

## 2. 로컬 개발 구조

```text
Developer Machine
  frontend: Next.js dev server
  backend: Spring Boot local server
  database: local PostgreSQL or Docker PostgreSQL
```

기본 포트 후보:

| 구성요소 | 포트 |
| --- | --- |
| frontend | 3000 |
| backend | 8080 |
| database | 5432 |

프론트엔드는 환경 변수로 백엔드 API 주소를 참조한다.

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

백엔드는 환경 변수로 DB 연결 정보를 참조한다.

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pickdeal
SPRING_DATASOURCE_USERNAME=pickdeal
SPRING_DATASOURCE_PASSWORD=pickdeal
```

## 3. 초기 운영 배포 후보

### 선택지 A: 단일 VPS + Docker Compose

```text
VPS
  reverse proxy
  frontend container
  backend container
  postgres container or managed DB
```

장점:

- 구조가 단순하다.
- 비용이 낮다.
- 개인용 서비스에 적합하다.
- 프론트엔드/백엔드/DB를 한 서버에서 관리할 수 있다.

단점:

- 서버 관리 책임이 있다.
- 장애 대응과 백업을 직접 구성해야 한다.

### 선택지 B: Frontend managed hosting + Backend VPS + Managed DB

```text
Vercel or similar
  frontend

VPS or container platform
  backend

Managed PostgreSQL
  database
```

장점:

- 프론트엔드 배포가 편하다.
- DB 백업과 운영 부담이 줄어든다.
- HTTPS와 CDN 구성이 쉽다.

단점:

- 서비스가 여러 곳으로 나뉘어 설정이 늘어난다.
- 무료/저가 플랜 제약을 확인해야 한다.

## 4. MVP 권장안

MVP에서는 선택지 A를 우선 권장한다.

```text
User Browser
  -> HTTPS Reverse Proxy
    -> Next.js Frontend
    -> Spring Boot Backend
      -> PostgreSQL
```

이유:

- 개인용 서비스에 충분하다.
- 배포 단위가 명확하다.
- 향후 Docker Compose에 collector worker를 추가하기 쉽다.
- 비용 예측이 쉽다.

## 5. 환경 구성

### frontend 환경 변수

| 이름 | 설명 |
| --- | --- |
| NEXT_PUBLIC_API_BASE_URL | 브라우저에서 호출할 백엔드 API 주소 |

### backend 환경 변수

| 이름 | 설명 |
| --- | --- |
| SPRING_DATASOURCE_URL | DB URL |
| SPRING_DATASOURCE_USERNAME | DB 사용자명 |
| SPRING_DATASOURCE_PASSWORD | DB 비밀번호 |
| APP_CORS_ALLOWED_ORIGINS | 허용할 프론트엔드 origin |

### future 환경 변수

| 이름 | 설명 |
| --- | --- |
| COLLECTOR_ENABLED | 수집기 활성화 여부 |
| COLLECTOR_INTERVAL_SECONDS | 수집 주기 |
| AI_SUMMARY_ENABLED | AI 요약 활성화 여부 |
| AI_PROVIDER | AI 제공자 |
| AI_MODEL | AI 모델 |

## 6. 데이터베이스 운영

초기 DB는 PostgreSQL을 권장한다.

운영 기준:

- 정기 백업을 설정한다.
- schema migration 도구를 사용한다.
- 수동 데이터 수정이 필요할 수 있으므로 DB 접속 절차를 문서화한다.
- 수집기 도입 후에는 수집 이력 테이블을 이용해 장애를 추적한다.

마이그레이션 도구 후보:

- Flyway
- Liquibase

MVP에서는 Spring Boot와 궁합이 단순한 Flyway를 우선 후보로 둔다.

## 7. 배포 파이프라인 후보

초기에는 수동 배포로 시작할 수 있다.

```text
git pull
  -> frontend build
  -> backend build
  -> docker compose up -d --build
```

추후 CI/CD 후보:

- GitHub Actions
- Docker image build
- VPS SSH deploy
- DB migration 자동 실행

## 8. 모니터링과 로그

MVP 최소 기준:

- 백엔드 애플리케이션 로그
- HTTP 에러 로그
- DB 백업 상태 확인
- 디스크 사용량 확인

수집기 추가 후 필요 항목:

- 출처별 수집 성공/실패율
- 수집 소요 시간
- 신규 핫딜 수집 건수
- AI 요약 성공/실패율
- 외부 사이트 요청 실패율

## 9. 향후 확장 배포 구조

```text
User Browser
  -> CDN / Reverse Proxy
    -> Frontend
    -> Backend API

Backend API
  -> PostgreSQL
  -> Redis

Collector Worker
  -> External Sites
  -> PostgreSQL
  -> Queue

AI Summary Worker
  -> Queue
  -> AI Provider
  -> PostgreSQL
```

확장 시점:

- 수집 작업이 API 서버 성능에 영향을 줄 때
- AI 요약 처리 시간이 길어질 때
- 알림, 큐, 재시도 처리가 필요해질 때
- 다중 사용자 기능이 추가될 때

