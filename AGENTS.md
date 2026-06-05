# AGENTS.md

PickDeal 프로젝트에서 작업할 때의 컨텍스트와 규칙. 상세 설계는 `docs/` 참고.

## 프로젝트 개요

- **PickDeal**: 핫딜 수집/조회 서비스 (MVP 단계)
- **Monorepo**: `frontend/` (Next.js, 스캐폴딩 + 목데이터 기반 화면 일부 구현, 백엔드 연동 전) + `backend/` (Spring Boot) + `docs/` (설계 문서)
- **설계 의도/정책**의 단일 진실 출처는 `docs/01~06`. 단, **실제 패키지 구조·라이브러리 버전**은 코드와 `build.gradle`/`package.json`이 진실이다(둘이 어긋나면 코드 기준으로 docs를 갱신).

## 스택

- **Backend**: Spring Boot 4.0.6, Java 17, Gradle, JPA, PostgreSQL (당장은 H2 in-memory로 기동, 개발 중 PostgreSQL로 전환 예정)
- **Frontend**: Next.js 16 App Router, TypeScript strict, Tailwind CSS
- **API prefix**: `/api/v1/*`

## 빌드 · 실행 · 테스트

- **Backend** (`backend/`):
  - 테스트: `./gradlew test` — H2 in-memory라 별도 DB 준비 불필요.
  - 실행: `./gradlew bootRun` (기본 포트 8080, H2 콘솔 `/h2-console`).
- **Frontend** (`frontend/`): 패키지 매니저는 **npm**(`package-lock.json`).
  - 설치/실행: `npm install` → `npm run dev`.
  - 현재 화면은 `lib/mock-data.ts` 기반이며 백엔드 API 연동 전 단계다.

## 백엔드 패키지 규칙

도메인 중심 + 계층별 하위 패키지. `com.pickdeal.{domain}/`:

- `api/` — Controller (`DealController`, 관리용은 `InternalDealController`)
- `application/` — Service (트랜잭션 경계, 도메인 규칙)
- `domain/` — Entity + Repository + Enum
- `dto/` — 요청/응답 분리 (`CreateDealRequest`, `DealDetailResponse` 등)

공통:
- `common/error/` — `BusinessException` + `ErrorCode` enum 기반. 신규 에러는 `ErrorCode`에 추가하고 `GlobalExceptionHandler`가 처리.
- `common/response/` — 응답은 `ApiResponse<T>`로 감싼다. 페이지는 `PageMetaResponse`.
- `config/` — `CorsConfig`, `SeedDataInitializer` 등 설정 빈.

도메인 추가 시 위 4계층 구조를 그대로 따른다.

- 도메인 패키지명은 API 리소스와 일치시킨다. 예: 관심/제외 키워드는 `keyword` 패키지(`KeywordController`, 엔티티 `Keyword`) ↔ `/api/v1/keywords`.

## 프론트엔드 규칙 (구현 시)

- 목록/상세는 **서버 컴포넌트 SSR 우선**, 상호작용 필요한 부분만 클라이언트 컴포넌트로 분리. (현재는 `lib/mock-data.ts` 기반 단계 — 백엔드 API 연동 시 이 원칙을 적용)
- API base URL은 `NEXT_PUBLIC_API_BASE_URL` 환경변수.
- 사용자 설정(키워드/출처 표시여부)은 **백엔드 DB가 SSOT**. localStorage에 저장 금지.
- (인증 도입 시) SSR/Route Handler에서 백엔드 호출에 세션 쿠키·CSRF 토큰 전달이 필요하면 Next 프록시 라우트를 경유. MVP는 인증이 없어 현재는 해당 없음.

## MVP 범위 (도입하지 않음)

다음은 의도적으로 보류 중이므로 추가 제안 전 확인:
- Redis (캐시/큐)
- 별도 collector worker 컨테이너
- 인증/멀티유저 (현재 단일 고정 user_id)
- 메시지 큐

`scheduler` 패키지는 아직 만들지 않았다. 2차 수집 단계에서 추가한다.

## 작업 시 유의

- 도메인 로직은 Service에 둔다. Controller는 DTO 매핑·검증·상태 코드만.
- 키워드/출처 필터링은 Service에서 쿼리 조건으로 반영 (`docs/01` 3.2 우선순위 규칙).
- DB 스키마/마이그레이션 변경은 `docs/04` 갱신과 함께.
- API 추가/변경은 `docs/03` 갱신과 함께.
