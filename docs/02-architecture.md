# 02. 아키텍처 설계 (Architecture)

> PickDeal — 전체 아키텍처 / 프론트엔드 화면 구조 / 백엔드 패키지 구조 / 확장 방향
> 작성 기준일: 2026-05-20

---

## 1. 기술 스택 및 버전 기준

> 버전은 프로젝트 생성 시점(2026-05) 기준의 stable을 따른다. 보안 패치가 반영된 최신 패치 버전을 사용한다.

### 1.1 Frontend

- **Next.js (App Router)** — 2026-05 기준 최신 stable은 **16.2.x (LTS)**.
  - Next.js 16부터 Turbopack과 React Compiler 지원이 stable로 기본 활성화된다.
  - App Router를 표준으로 사용한다(Pages Router 사용하지 않음).
- **TypeScript** — strict 모드 사용.
- **Tailwind CSS** — 유틸리티 기반 스타일링.
- 배포: **Vercel** 기본(`docs/06`).

### 1.2 Backend

- **Spring Boot (REST API)** — 현재 구현은 **Spring Boot 4.0.x(4.0.6)** 와 **JDK 17**을 기준으로 한다.
- **Java**: JDK 17 이상. 현재 프로젝트 toolchain은 JDK 17(Spring Boot 4.0의 baseline)을 사용한다.
- **빌드**: Gradle. 본 문서는 Gradle을 가정해 패키지 구조를 기술한다.
- 정확한 의존성/패치 버전의 단일 출처는 `backend/build.gradle`이다.

### 1.3 Database

- **표준 DBMS는 PostgreSQL**(개발·운영 공통). MySQL 선택 시 장단점은 `docs/04-database-design.md` 참고.
- **현재는 잠정적으로 H2 in-memory(PostgreSQL 호환 모드)로 기동**한다(`ddl-auto: create-drop`). 초기 개발 중 노트북에서 DB와 애플리케이션을 함께 돌릴 때의 부하를 줄이기 위한 임시 조치다.
- **개발 진행 중 PostgreSQL로 전환할 계획**이다. PostgreSQL 드라이버는 이미 의존성에 포함돼 있어 프로파일/접속 정보만 바꾸면 된다. 운영 PostgreSQL은 Docker Compose로 실행(`docs/06`).

### 1.4 도입하지 않는 것 (MVP)

- Redis: MVP 필수 아님. 2차 구성으로 도입(`docs/05`).
- 메시지 큐, 별도 collector worker: 확장 단계에서 분리.

---

## 2. 전체 아키텍처

### 2.1 MVP 아키텍처 (단일 백엔드 + 단일 DB)

```
┌──────────────────────────────┐
│           사용자(브라우저)        │
└───────────────┬──────────────┘
                │ HTTPS
        ┌───────▼────────┐
        │   Frontend      │   Next.js App Router (Vercel)
        │   (SSR/CSR)     │   - 목록/상세 화면, 설정 화면
        └───────┬────────┘
                │ REST (/api/v1/*)
        ┌───────▼────────────────────────┐
        │   Backend (Spring Boot)          │  OCI/VPS, Docker Compose
        │  ┌────────────────────────────┐  │
        │  │ REST API (controller)       │  │
        │  │ 도메인 서비스 (service)        │  │
        │  │ 영속성 (repository, JPA)      │  │
        │  │ scheduler (동일 앱 내, 미사용→예약)│  │
        │  └────────────────────────────┘  │
        └───────┬────────────────────────┘
                │ JDBC
        ┌───────▼────────┐
        │  PostgreSQL     │   Docker Compose (로컬/운영)
        └────────────────┘
```

- MVP에서는 **backend와 scheduler를 하나의 Spring Boot 애플리케이션**으로 둔다. scheduler는 자리만 마련하고, 실제 수집 작업은 비활성/미구현 상태로 둔다.
- frontend는 backend REST API(`/api/v1/*`)만 호출한다.

### 2.2 확장 아키텍처 (2차: collector worker 분리)

```
                ┌────────────┐
   Vercel ──────│  Frontend   │
                └─────┬──────┘
                      │ REST
                ┌─────▼───────┐        ┌──────────────┐
                │  Backend API │◀──────│ Redis (캐시/큐/ │
                │ (조회·설정 전담)│        │ rate limit/dedup)│
                └─────┬───────┘        └──────▲───────┘
                      │ JDBC                   │
                ┌─────▼───────┐         ┌──────┴────────┐
                │ PostgreSQL   │◀────────│ Collector Worker│ (별도 컨테이너)
                └─────────────┘  insert  │ - 출처별 수집     │
                                          │ - 정규화/dedup   │
                                          │ - (3차) AI 요약  │
                                          └─────────────────┘
```

- 수집 대상이 늘어나면 **collector worker를 별도 컨테이너로 분리**한다.
- Backend API는 조회/설정에 집중하고, Worker는 수집·정규화·중복 제거를 담당한다.
- Redis는 이 시점에 캐시/작업 큐/중복 수집 방지/rate limit 용도로 도입한다.
- 상세는 `docs/05-collector-design.md`, 컨테이너 구성은 `docs/06-deployment.md` 참고.

---

## 3. Monorepo 구조

```
pick-deal/
├─ frontend/                  # Next.js App Router (TypeScript, Tailwind)
├─ backend/                   # Spring Boot REST API (+ scheduler, 동일 앱)
├─ docs/                      # 설계 문서 (본 문서들)
│  ├─ 01-requirements.md
│  ├─ 02-architecture.md
│  ├─ 03-api-design.md
│  ├─ 04-database-design.md
│  ├─ 05-collector-design.md
│  └─ 06-deployment.md
└─ README.md                 # (추후 작성)
```

- 하나의 저장소에서 frontend/backend를 폴더로 분리한다.
- 빌드/배포 파이프라인은 각 폴더 단위로 독립적으로 동작하도록 설계한다(frontend→Vercel, backend→Docker).
- 현재 레포에는 `backend/` Spring Boot 코드가 구현돼 있고, `frontend/`는 Next.js 프로젝트가 스캐폴딩된 상태다(레이아웃/홈 골격까지, 4장 화면은 점진 구현).

---

## 4. 프론트엔드 화면 구조 (Next.js App Router)

### 4.1 라우트 구조 (초안)

```
frontend/
└─ app/
   ├─ layout.tsx               # 공통 레이아웃 (헤더/네비)
   ├─ page.tsx                 # 핫딜 목록 (홈)
   ├─ deals/
   │  └─ [id]/
   │     └─ page.tsx           # 핫딜 상세
   └─ settings/
      ├─ page.tsx              # 설정 메인 (탭)
      ├─ sources/
      │  └─ page.tsx           # 출처 표시/숨김 설정
      └─ keywords/
         └─ page.tsx           # 관심/제외 키워드 관리
```

### 4.2 화면별 정의

| 화면 | 경로 | 설명 | 사용 API |
| --- | --- | --- | --- |
| 핫딜 목록 | `/` | 필터(출처/정렬) + 무한스크롤 또는 페이지네이션. 숨김 출처/제외 키워드 자동 반영 | `GET /api/v1/deals` |
| 핫딜 상세 | `/deals/[id]` | 단일 딜 상세 + 원본 링크 이동 | `GET /api/v1/deals/{id}` |
| 출처 설정 | `/settings/sources` | 출처 목록 + 표시/숨김 토글 | `GET /api/v1/sources`, `PATCH /api/v1/sources/{id}/visibility` |
| 키워드 설정 | `/settings/keywords` | 관심/제외 키워드 등록·조회·삭제 | `GET/POST/DELETE /api/v1/keywords` |

### 4.3 컴포넌트 구성 (초안)

- `components/deal/DealCard.tsx` — 목록의 개별 딜 카드
- `components/deal/DealList.tsx` — 목록 + 페이지네이션/무한스크롤
- `components/deal/DealFilterBar.tsx` — 출처 필터/정렬 컨트롤
- `components/source/SourceToggleItem.tsx` — 출처 표시/숨김 토글 행
- `components/keyword/KeywordManager.tsx` — 키워드 입력/리스트/삭제
- `components/common/*` — 버튼, 배지(할인율 등), 빈 상태, 에러 표시

### 4.4 데이터 패칭 원칙

- 목록/상세는 서버 컴포넌트에서 fetch(SSR) 우선, 상호작용이 필요한 필터/설정은 클라이언트 컴포넌트로 분리.
- API base URL은 환경변수(`NEXT_PUBLIC_API_BASE_URL`)로 주입한다.
- 키워드/출처 설정은 **백엔드 DB가 단일 소스 오브 트루스(SSOT)** 다. localStorage에 설정을 저장하지 않는다.

---

## 5. 백엔드 패키지 구조 (Spring Boot)

> 도메인 중심 + 계층별 하위 패키지(`{domain}/api·application·domain·dto`). 단일 애플리케이션으로 시작한다.
> 아래는 **현재 구현된 실제 구조**다. 도메인 추가 시 동일한 4계층을 따른다(`AGENTS.md` 참고).

```
backend/
└─ src/main/java/com/pickdeal/
   ├─ PickDealApplication.java          # @SpringBootApplication
   ├─ common/
   │  ├─ domain/BaseTimeEntity.java     # 공통 시간 컬럼(생성/수정)
   │  ├─ response/                      # ApiResponse, PageMetaResponse, ErrorResponse
   │  └─ error/                         # ErrorCode(enum) + BusinessException 계열
   │     ├─ ErrorCode.java
   │     ├─ BusinessException.java
   │     ├─ ResourceNotFoundException.java
   │     ├─ DuplicateResourceException.java
   │     └─ GlobalExceptionHandler.java
   ├─ config/                           # CorsConfig, SeedDataInitializer
   ├─ deal/                             # 딜 도메인
   │  ├─ api/                           # DealController, InternalDealController
   │  ├─ application/                   # DealService
   │  ├─ domain/                        # Deal(엔티티), DealRepository, DealStatus
   │  └─ dto/                           # CreateDealRequest, DealSummary/Detail/ListResponse
   ├─ source/                           # 출처 도메인 (+ 표시/숨김 설정)
   │  ├─ api/                           # SourceController
   │  ├─ application/                   # SourceService
   │  ├─ domain/                        # Source, SourceVisibility, 각 Repository
   │  └─ dto/                           # SourceResponse, UpdateSourceVisibilityRequest
   └─ preference/                       # 사용자 환경설정 도메인 — 현재는 관심/제외 키워드
      │                                 #   (API 리소스명은 /api/v1/keywords)
      │                                 #   2차의 알림 구독 설정 등도 이 도메인에 흡수 예정
      ├─ api/                           # PreferenceController
      ├─ application/                   # PreferenceService
      ├─ domain/                        # PreferenceKeyword, PreferenceKeywordRepository, KeywordType
      └─ dto/                           # CreateKeywordRequest, KeywordResponse
```

- **scheduler 패키지는 아직 만들지 않았다.** 2차 수집 단계에서 `@Scheduled` 잡 또는 별도 worker로 추가한다(`docs/05`).

```
backend/
└─ src/main/resources/
   └─ application.yml                   # 현재: H2 in-memory + ddl-auto create-drop
```

- **현재 시드는 코드 기반**(`config/SeedDataInitializer`)으로 적재하며, SQL 마이그레이션 파일은 두지 않는다.
- PostgreSQL 전환 시 추가할 **계획**(미작성): 프로파일 분리(`application-local.yml`/`application-prod.yml`)와 Flyway 마이그레이션(`db/migration/V1__init.sql`). 상세는 `docs/04` 1장.

### 5.1 계층 책임

- **Controller**: 요청/응답 DTO 매핑, 입력 검증, HTTP 상태 코드. 비즈니스 로직 없음.
- **Service**: 도메인 규칙(키워드 필터링 우선순위 등). 트랜잭션 경계.
- **Repository**: JPA. 동적 필터/정렬은 `Specification` 또는 QueryDSL 고려(택1, 구현 단계 결정).
- **scheduler**: 현재 미생성. 2차 수집 단계에서 `@Scheduled` 잡으로 추가하거나 worker로 분리한다(`docs/05`).

### 5.2 키워드 필터링 위치

- 키워드/출처 필터링은 **Service 계층에서 쿼리 조건으로** 반영한다(`docs/01` 3.2의 우선순위 규칙 준수).
- 데이터량이 적은 MVP에서는 DB의 `ILIKE`(PostgreSQL) / `LIKE`(MySQL) 기반 단순 포함 검색으로 충분하다.

---

## 6. 확장 방향 요약

| 항목 | MVP | 확장 |
| --- | --- | --- |
| 수집 | 더미/수동 데이터 | collector 자동 수집 (`docs/05`) |
| scheduler | 단일 앱 내 비활성 | worker 분리 가능 (`docs/02` 2.2, `docs/06`) |
| 캐시/큐 | 없음 | Redis 도입 (`docs/05`) |
| 중복 제거 | 스키마만 대비 | dedup 로직 활성 (`docs/04`, `docs/05`) |
| AI | 없음 | 댓글 요약·구매 판단 보조 (`docs/05`) |
| 사용자 | 단일(고정 user_id) | 멀티유저 + 인증 |

---

## 7. 관련 문서

- API 규약/엔드포인트: `docs/03-api-design.md`
- 테이블/인덱스/제약: `docs/04-database-design.md`
- 수집기·AI·Redis·worker: `docs/05-collector-design.md`
- 배포·로컬환경·구현 순서: `docs/06-deployment.md`
