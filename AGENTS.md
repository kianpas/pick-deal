# AGENTS.md

PickDeal 프로젝트에서 작업할 때의 컨텍스트와 규칙. 상세 설계는 `docs/` 참고.

## 프로젝트 개요

- **PickDeal**: 핫딜 수집/조회 서비스 (MVP 단계)
- **Monorepo**: `frontend/` (Next.js, 목록·상세·키워드 설정 백엔드 연동 완료, 출처 설정 화면 미구현) + `backend/` (Spring Boot REST API + Quasarzone 수집기) + `docs/` (설계 문서)
- **설계 의도/정책**의 단일 진실 출처는 `docs/01~06`. 단, **실제 패키지 구조·라이브러리 버전**은 코드와 `build.gradle`/`package.json`이 진실이다(둘이 어긋나면 코드 기준으로 docs를 갱신).
- **문서 맵** — 용어·재사용 자산은 `CONTEXT.md`, 되돌리기 비싼 결정의 이력은 `docs/adr/`, 상세 설계는 `docs/01~06`, 지금 일하는 규칙은 이 파일(AGENTS.md).

## 설계 원칙 (설계·문서 요청 시 반드시 적용)

> 이 프로젝트는 **단일 사용자 핫딜 뷰어 MVP**다. 과설계는 결함으로 취급한다.

- **범위에 비례한 격식.** MVP 작업엔 MVP 분량의 설계만 한다. 문서를 균일한 밀도로 채우지 말 것 — 계약(API/스키마)엔 깊게, 먼 미래(2차·3차)엔 한 문단. 균일한 성실함은 생성물의 냄새다.
- **비용 비대칭으로 칼질한다.** "이걸 나중에 하면 지금보다 얼마나 더 비싼가?"를 기준으로:
  - *지금 한다* — 나중에 붙이면 비싼 것: 데이터 정합성 제약(유니크/FK), 공개 API 계약, 되돌리기 힘든 경계. (예: `source_id + external_id` 유니크)
  - *미룬다* — 나중에 싸게 붙는 것: 미래 기능 풀설계, 안 쓰는 추상 레이어, 인프라 분리(Redis/worker/nginx).
  - 단, **실제로 만들 계획이 선 기능은 "미래"가 아니다.** 예: 기본 회원가입/로그인은 계획된 마일스톤이라 `user_id`를 지금 유지한다.
- **미래 단계는 "방향 한 문단"까지만.** 아직 안 만든 것을 완성형으로 설계하지 않는다(붙일 때 다시 쓰게 된다).
- **환경 패리티.** 개발/운영 DB는 같은 것을 권장. 임시 우회(H2 등)는 리스크를 명시한다.
- **절제도 산출물이다.** 안 쓴 것으로 판단을 보여라. 새 문서/추상화를 만들기 전에 "지금 필요한가? 더 짧게 끝낼 수 있나?"를 먼저 자문한다.

## 스택

- **Backend**: Spring Boot 4.0.6, Java 17, Gradle, JPA, PostgreSQL (로컬 `pickdeal` DB로 기동, 접속 정보는 `DB_USERNAME`/`DB_PASSWORD` 환경변수로 오버라이드. 테스트만 H2 in-memory)
- **Frontend**: Next.js 16 App Router, TypeScript strict, Tailwind CSS
- **API prefix**: `/api/v1/*`

## 빌드 · 실행 · 테스트

- **Backend** (`backend/`):
  - 테스트: `./gradlew test` — H2 in-memory라 별도 DB 준비 불필요.
  - 실행: `./gradlew bootRun` (기본 포트 8080, 로컬 PostgreSQL `pickdeal` DB 필요).
- **Frontend** (`frontend/`): 패키지 매니저는 **npm**(`package-lock.json`).
  - 설치/실행: `npm install` → `npm run dev`.
  - 목록(`/`), 상세(`/deals/[id]`), 키워드 설정(`/settings/keywords`)은 `lib/api.ts`를 통해 백엔드 API와 연동한다.
  - 출처 설정 API 클라이언트는 구현됐지만 `/settings/sources` 화면은 아직 없다.
  - `lib/mock-data.ts`와 `lib/types.ts`는 데모/레거시 자산이며 신규 화면의 계약 타입은 `lib/api-types.ts`를 사용한다.

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

## 현재 단계와 도입하지 않는 것

핵심 조회·설정 API 이후 **2차 수집기의 첫 출처(Quasarzone)까지 구현된 상태**다. 다음은 의도적으로 보류 중이므로 추가 제안 전 확인:
- Redis (캐시/큐)
- 별도 collector worker 컨테이너
- 인증/멀티유저 (현재 단일 고정 user_id)
- 메시지 큐

수집기는 `collector/` 패키지에 있다. 출처별 하위 패키지(`collector/quasarzone/`)에 fetch(Client)·parse(Parser)·normalize/persist(CollectService)를 두고, `CollectScheduler`(`@Scheduled`)가 기본 활성 상태로 주기 실행한다. 새 출처는 우선 새 하위 패키지로 추가하고, 공통 인터페이스는 두 번째 출처에서 실제 중복이 확인될 때 추출한다. 파서는 `String html → 결과` 순수 함수로 두고 실제 응답 HTML 픽스처(`src/test/resources/fixtures/`)로 테스트한다.

## 작업 시 유의

- 도메인 로직은 Service에 둔다. Controller는 DTO 매핑·검증·상태 코드만.
- 키워드/출처 필터링은 Service에서 쿼리 조건으로 반영 (`docs/01` 3.2 우선순위 규칙).
- DB 스키마/마이그레이션 변경은 `docs/04` 갱신과 함께.
- API 추가/변경은 `docs/03` 갱신과 함께.
- 필터 우선순위·API 계약 변경은 해당 Service/Controller 테스트와 함께 반영한다.
