# AGENTS.md

PickDeal 프로젝트에서 작업할 때의 컨텍스트와 규칙. 상세 설계는 `docs/` 참고.

## 프로젝트 개요

- **PickDeal**: 핫딜 수집/조회 서비스 (MVP 단계)
- **Monorepo**: `frontend/` (Next.js, 미구현) + `backend/` (Spring Boot) + `docs/` (설계 문서)
- 설계 결정의 단일 진실 출처는 `docs/01~06`. 구조나 정책이 모호하면 코드보다 docs를 우선 확인.

## 스택

- **Backend**: Spring Boot 3.5.14, Java 17, Gradle, JPA, PostgreSQL (로컬은 H2 가능)
- **Frontend**: Next.js 16 App Router, TypeScript strict, Tailwind CSS
- **API prefix**: `/api/v1/*`

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

## 프론트엔드 규칙 (구현 시)

- 목록/상세는 **서버 컴포넌트 SSR 우선**, 상호작용 필요한 부분만 클라이언트 컴포넌트로 분리.
- API base URL은 `NEXT_PUBLIC_API_BASE_URL` 환경변수.
- 사용자 설정(키워드/출처 표시여부)은 **백엔드 DB가 SSOT**. localStorage에 저장 금지.
- SSR/Route Handler에서 백엔드 호출 시 세션 쿠키·CSRF 토큰 전달이 필요한 경로면 Next 프록시 라우트를 경유.

## MVP 범위 (도입하지 않음)

다음은 의도적으로 보류 중이므로 추가 제안 전 확인:
- Redis (캐시/큐)
- 별도 collector worker 컨테이너
- 인증/멀티유저 (현재 단일 고정 user_id)
- 메시지 큐

`scheduler` 패키지는 자리만 마련하고 동작시키지 않는다.

## 작업 시 유의

- 도메인 로직은 Service에 둔다. Controller는 DTO 매핑·검증·상태 코드만.
- 키워드/출처 필터링은 Service에서 쿼리 조건으로 반영 (`docs/01` 3.2 우선순위 규칙).
- DB 스키마/마이그레이션 변경은 `docs/04` 갱신과 함께.
- API 추가/변경은 `docs/03` 갱신과 함께.
