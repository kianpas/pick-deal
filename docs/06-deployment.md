# 06. 배포 설계 (Deployment)

> PickDeal — 배포 전략 / Docker 기반 확장 방향 / 로컬 개발 환경 / Codex 구현 작업 순서
> Dockerfile, docker-compose.yml, nginx 설정, CI/CD 스크립트는 아직 없다. 이 문서는 **현재 로컬 실행법**과 **향후 배포 방향**을 구분한다.
> 최초 작성: 2026-05-20 · 현재 상태 갱신: 2026-07-11

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

## 2. 현재 로컬 개발 환경

- **DB는 로컬 PostgreSQL의 `pickdeal` DB를 사용**한다. 현재 Docker Compose 구성은 없다.
- frontend: 로컬에서 `next dev`(Turbopack)로 실행, `NEXT_PUBLIC_API_BASE_URL`을 로컬 backend로 지정.
- backend: 단일 `application.yml`로 실행하며 `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`로 접속 정보를 덮어쓴다.
- 테스트: H2 in-memory를 사용하며 실제 수집 scheduler를 비활성화한다.
- 시드 데이터는 현재 별도 프로파일 구분 없이, 필요한 기준 데이터가 없을 때 코드에서 적재한다(`docs/04` 6장).

> 아래는 향후 구성할 서비스의 방향이다. 현재 실행 명령으로 오해하지 않는다.

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
| Backend | `DB_NAME` | DB 이름(기본값 `pickdeal`) |
| Backend | `DB_USERNAME`, `DB_PASSWORD` | DB 접속 계정 |

`SPRING_PROFILES_ACTIVE`과 `DB_URL`은 현재 설정에서 사용하지 않는다. 프로파일 분리나 전체 JDBC URL 주입이 필요해질 때 코드와 함께 추가한다.

- 비밀값은 저장소에 커밋하지 않는다(Vercel/OCI 시크릿 사용). `.env.example`은 구현 단계에서 추가.
- Redis/LLM 등 2차·3차 환경변수는 **그 기능을 실제로 붙일 때** 추가한다(지금 정의하지 않음).

---

## 4. CI/CD · 컨테이너 확장 (방향만)

- **Frontend**: Vercel Git 연동 자동 배포(프리뷰/프로덕션).
- **Backend**: 이미지 빌드 → 레지스트리 → OCI에서 `compose pull && up`. 스크립트는 구현 단계에서 작성.
- **redis/worker/nginx 분리**는 부하가 실제로 그것을 요구할 때 추가한다. 기준·구성은 그 시점에 정하고, 지금 미리 설계하지 않는다(공유 PostgreSQL + 필요 시 Redis 큐 정도만 방향으로 둔다).

---

## 5. 현재 진행 상태와 다음 작업

> 0~6과 프론트 목록·상세·키워드 화면, Quasarzone 수집기는 구현됐다. 아래 항목은 남은 작업을 판단하기 위한 상태 기록이다.

0. **레포 스캐폴딩**
   - `pick-deal/` monorepo `frontend/`, `backend/` 구성, 루트 README/`.gitignore` 정리.
1. **Backend 프로젝트 생성**
   - Spring Boot 프로젝트 생성(버전: `docs/02` 1.2 기준 — 현재 구현은 Spring Boot 4.0.6 + JDK 17).
   - 패키지 구조(`docs/02` 5장), 공통 응답/에러 핸들러, CORS 설정.
2. **DB 스키마 & 엔티티**
   - JPA 엔티티/리포지토리 작성. 현재 스키마는 `ddl-auto`로 생성한다(`source`, `deal`, `source_visibility`, `keyword` — 인덱스/유니크 포함, `docs/04` 2장).
   - Flyway `V1__init.sql` 도입은 운영 배포 준비 시 진행한다(현재 미작성, `docs/04` 1장).
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
   - **미구현**. 로컬 DB 실행용 compose와 backend 프로파일 분리는 필요성이 생기거나 배포 준비 시 함께 작성한다.
8. **Frontend 프로젝트 생성 & 화면**
   - Next.js(App Router, TS, Tailwind) 생성(버전 `docs/02` 1.1).
   - 화면: 목록(`/`), 상세(`/deals/[id]`), 사이드바 출처 설정, 키워드 설정(`/settings/keywords`) (`docs/02` 4장).
   - 목록·상세·키워드 설정과 사이드바 출처 표시/숨김 API 연동 완료. 일부 빈 상태/에러 UX는 보완 여지가 있다.
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
