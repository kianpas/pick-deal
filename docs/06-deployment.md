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

## 3. 환경 변수 / 설정 (현재 필요한 것만)

| 영역 | 키 | 설명 |
| --- | --- | --- |
| Frontend | `NEXT_PUBLIC_API_BASE_URL` | backend API base URL |
| Backend | `SPRING_PROFILES_ACTIVE` | `local` / `prod` |
| Backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | DB 접속 정보 |

- 비밀값은 저장소에 커밋하지 않는다(Vercel/OCI 시크릿 사용). `.env.example`은 구현 단계에서 추가.
- Redis/LLM 등 2차·3차 환경변수는 **그 기능을 실제로 붙일 때** 추가한다(지금 정의하지 않음).

---

## 4. CI/CD · 컨테이너 확장 (방향만)

- **Frontend**: Vercel Git 연동 자동 배포(프리뷰/프로덕션).
- **Backend**: 이미지 빌드 → 레지스트리 → OCI에서 `compose pull && up`. 스크립트는 구현 단계에서 작성.
- **redis/worker/nginx 분리**는 부하가 실제로 그것을 요구할 때 추가한다. 기준·구성은 그 시점에 정하고, 지금 미리 설계하지 않는다(공유 PostgreSQL + 필요 시 Redis 큐 정도만 방향으로 둔다).

---

## 5. 구현 작업 순서 (MVP)

> 아래 순서대로 구현한다. 각 단계는 앞 단계 산출물에 의존한다.

0. **레포 스캐폴딩**
   - `pick-deal/` monorepo `frontend/`, `backend/` 구성, 루트 README/`.gitignore` 정리.
1. **Backend 프로젝트 생성**
   - Spring Boot 프로젝트 생성(버전: `docs/02` 1.2 기준 — 현재 구현은 Spring Boot 4.0.6 + JDK 17).
   - 패키지 구조(`docs/02` 5장), 공통 응답/에러 핸들러, CORS 설정.
2. **DB 스키마 & 엔티티**
   - JPA 엔티티/리포지토리 작성. 현재 스키마는 `ddl-auto`로 생성한다(`source`, `deal`, `source_visibility`, `keyword` — 인덱스/유니크 포함, `docs/04` 2장).
   - Flyway `V1__init.sql` 도입은 PostgreSQL 전환 시 진행한다(현재 미작성, `docs/04` 1장).
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

> **확장(2차 이후)**: 수집기 → dedup → Redis/worker → 알림 → AI 요약/판단 → 멀티유저 순으로 붙인다. 각 단계는 **그 단계에 착수할 때** 설계·구현한다. 지금은 순서만 안다(방향은 `docs/05`).

---

## 6. 관련 문서

- 전체/확장 아키텍처: `docs/02-architecture.md`
- API: `docs/03-api-design.md`
- DB/시드: `docs/04-database-design.md`
- 수집기 확장 방향(최소 문서): `docs/05-collector-design.md`
