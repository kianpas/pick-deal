# 06. 배포 설계 (Deployment)

> PickDeal — 배포 전략 / Docker 기반 확장 방향 / 로컬 개발 환경 / Codex 구현 작업 순서
> **이번 단계에서는 Dockerfile, docker-compose.yml, nginx 설정, CI/CD 스크립트를 작성하지 않는다.** 구조와 방향만 문서화한다.
> 작성 기준일: 2026-05-20

---

## 1. 배포 전략 개요 (Vercel + OCI)

```
   사용자
     │ HTTPS
     ▼
┌─────────────┐        ┌──────────────────────────────┐
│  Vercel      │  REST  │  OCI / VPS (Docker Compose)    │
│  Frontend    │ ─────▶ │  ┌────────────┐  ┌──────────┐ │
│  (Next.js)   │        │  │ backend     │  │ postgres  │ │
└─────────────┘        │  │ (Spring Boot)│─▶│ (DB)      │ │
                       │  └────────────┘  └──────────┘ │
                       │   (+ 2차: nginx, redis, worker) │
                       └──────────────────────────────┘
```

- **Frontend**: Vercel 배포를 기본으로 한다(Next.js App Router에 최적). 환경변수로 backend API base URL 주입.
- **Backend + DB**: OCI 또는 VPS에서 **Docker Compose**로 운영한다.
- **운영 기본 방향**: Vercel(frontend) + OCI Docker Compose(backend/DB).
- 도메인/TLS: 운영 시 backend 앞단에 리버스 프록시(nginx) + TLS를 둘 수 있다(2차, 본 단계에선 미작성).

---

## 2. 로컬 개발 환경

- **DB는 Docker Compose로 실행**한다(개발자는 PostgreSQL을 로컬에 직접 설치하지 않아도 됨).
- frontend: 로컬에서 `next dev`(Turbopack)로 실행, `NEXT_PUBLIC_API_BASE_URL`을 로컬 backend로 지정.
- backend: 로컬 프로파일(`application-local.yml`)로 실행, Docker Compose DB에 접속.
- 시드 데이터는 로컬/개발 프로파일에서만 적재한다(`docs/04` 6장).

> 구체적인 `docker-compose.yml`은 이번 단계에서 작성하지 않는다. 아래는 향후 구성할 서비스 구성의 "방향"만 기술한다.

향후 로컬 compose 구성 방향(미작성, 참고용):

```
services:
  db        # PostgreSQL (로컬/운영 공통 베이스)
  backend   # Spring Boot (로컬에선 외부 실행도 가능)
  # 2차 확장 시 추가:
  # redis
  # worker  (collector)
  # nginx   (운영 리버스 프록시/TLS)
```

---

## 3. Docker 기반 배포 확장 방향

> 단계적으로 컨테이너를 추가/분리할 수 있도록 설계한다. 각 단계는 필요 시점에 진행한다.

| 단계 | 컨테이너 구성 | 설명 |
| --- | --- | --- |
| **MVP 운영** | `backend`, `db` | 단일 백엔드(+scheduler 비활성) + PostgreSQL |
| **2차-a** | `+ redis` | 캐시/큐/중복 방지/rate limit 필요 시 Redis 추가 (`docs/05` 4장) |
| **2차-b** | `+ worker` | collector worker 분리. backend는 조회·설정 전담, worker는 수집·정규화·dedup (`docs/05` 6장) |
| **2차-c** | `+ nginx` | 리버스 프록시 + TLS, 라우팅/정적 처리 |

- 분리 시에도 **공유 PostgreSQL**을 기본으로 하고, 작업 분배가 필요하면 Redis 큐를 매개로 한다.
- worker는 무상태로 두어 인스턴스 수평 확장이 가능하게 한다.
- 각 단계의 Dockerfile/compose/nginx 파일은 해당 단계 착수 시 작성한다(현 단계 작성 금지).

---

## 4. 환경 변수 / 설정 (방향)

| 영역 | 키(예시) | 설명 |
| --- | --- | --- |
| Frontend | `NEXT_PUBLIC_API_BASE_URL` | backend API base URL |
| Backend | `SPRING_PROFILES_ACTIVE` | `local` / `prod` |
| Backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | DB 접속 정보 |
| Backend(2차) | `REDIS_HOST`, `REDIS_PORT` | Redis 도입 시 |
| Backend(3차) | `LLM_API_KEY`, `LLM_MODEL` | AI 요약/판단 도입 시 |

- 비밀값은 저장소에 커밋하지 않는다(Vercel/OCI의 시크릿/환경변수 사용). `.env.example`은 구현 단계에서 추가.

---

## 5. CI/CD (방향만)

> 본 단계에서는 스크립트를 작성하지 않는다.

- **Frontend**: Vercel의 Git 연동 자동 배포(프리뷰/프로덕션).
- **Backend**: 이미지 빌드 → 레지스트리 푸시 → OCI에서 compose pull & up. CI 파이프라인은 구현 단계에서 정의.
- monorepo이므로 변경 경로(`frontend/**`, `backend/**`)에 따라 파이프라인을 분기하도록 설계할 수 있다.

---

## 6. 이후 Codex가 구현할 작업 순서

> 본 설계 문서를 기준으로, 아래 순서대로 구현한다. 각 단계는 앞 단계 산출물에 의존한다.
> **0~9단계가 MVP, 10단계 이후는 확장.**

### MVP 구현

0. **레포 스캐폴딩**
   - `pick-deal/` monorepo에 `frontend/`, `backend/` 디렉터리 생성(이번 문서 단계에서는 만들지 않음).
   - 루트 README, `.gitignore` 정리.
1. **Backend 프로젝트 생성**
   - Spring Boot 프로젝트 생성(버전: `docs/02` 1.2 기준 — 현재 구현은 Spring Boot 4.0.6 + JDK 17).
   - 패키지 구조(`docs/02` 5장), 공통 응답/에러 핸들러, CORS 설정.
2. **DB 스키마 & 엔티티**
   - JPA 엔티티/리포지토리 작성. 현재 스키마는 `ddl-auto`로 생성한다(`source`, `deal`, `source_visibility`, `keyword` — 인덱스/유니크 포함, `docs/04` 2장).
   - Flyway `V1__init.sql` 도입은 운영 PostgreSQL 전환 시 진행한다(현재 미작성, `docs/04` 1장).
3. **시드 데이터**
   - 로컬/개발 프로파일용 더미 출처·딜 시드 적재(`docs/04` 6장).
4. **딜 API**
   - `GET /api/v1/deals`(필터/정렬/페이지네이션 + 숨김 출처·키워드 규칙), `GET /api/v1/deals/{id}` 구현(`docs/03` 2장, 필터 규칙 `docs/01` 3.2 / `docs/04` 4장).
   - (선택) `POST /api/v1/internal/deals`.
5. **출처 API**
   - `GET /api/v1/sources`, `PATCH /api/v1/sources/{id}/visibility`(`docs/03` 3장).
6. **키워드 API**
   - `GET/POST/DELETE /api/v1/keywords`(`docs/03` 4장, 중복/검증 처리).
7. **로컬 Docker Compose(DB)**
   - 로컬 DB 실행용 compose 작성(이 시점에 작성). backend가 접속하도록 프로파일 구성.
8. **Frontend 프로젝트 생성 & 화면**
   - Next.js(App Router, TS, Tailwind) 생성(버전 `docs/02` 1.1).
   - 화면: 목록(`/`), 상세(`/deals/[id]`), 출처 설정(`/settings/sources`), 키워드 설정(`/settings/keywords`) (`docs/02` 4장).
   - API 연동, 빈 상태/에러 처리.
9. **MVP 통합 & 배포 준비**
   - frontend↔backend 통합 동작 확인, 환경변수 정리.
   - frontend Vercel 배포, backend/DB OCI Docker Compose 배포 구성 작성(이 시점에 Dockerfile/compose/nginx 작성).

### 확장 구현 (10단계 이후)

10. **수집기(2차)**: scheduler 기반 수집 → 정규화 파이프라인(`docs/05` 2장).
11. **중복 제거(2차)**: `title_norm_hash`/그룹핑, dedup 로직(`docs/05` 3장).
12. **Redis(2차)**: 캐시/큐/중복 방지/rate limit 도입(`docs/05` 4장).
13. **worker 분리(2차)**: collector worker 컨테이너 분리(`docs/05` 6장, `docs/06` 3장).
14. **알림(2차)**: 관심 키워드 알림(`docs/05` 7장).
15. **AI 댓글 요약(3차)**: 댓글 수집·요약·노출(`docs/05` 5.1).
16. **AI 구매 판단 보조(3차)**: 보조 점수/근거(`docs/05` 5.2).
17. **멀티유저 전환(3차)**: `user` 테이블/인증 도입, 설정 API에 인증 적용(`docs/04` 7장, `docs/03` 5장).

---

## 7. 관련 문서

- 전체/확장 아키텍처: `docs/02-architecture.md`
- API: `docs/03-api-design.md`
- DB/시드: `docs/04-database-design.md`
- 수집기/AI/Redis/worker: `docs/05-collector-design.md`
