# AGENTS.md

PickDeal 프로젝트에서 작업할 때의 컨텍스트와 규칙. 상세 설계는 `docs/` 참고.

## 프로젝트 개요

- **PickDeal**: 핫딜 수집/조회 서비스 (MVP 단계)
- **Monorepo**: `frontend/` (Next.js, 목록·상세·키워드 설정 및 사이드바 출처 설정 백엔드 연동 완료) + `backend/` (Spring Boot REST API + 복수 출처 수집기) + `docs/` (설계 문서)
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
  - 출처 표시/숨김은 별도 페이지가 아니라 `LeftSidebar`의 출처 영역에서 변경하며, 백엔드 DB를 SSOT로 사용한다.
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

### 디자인 규칙

`app/globals.css`가 토큰의 단일 출처다(색·폰트). 코드가 표현하지 못하는 관례만 여기 적는다.

- **색은 토큰 경유.** 컴포넌트에 hex/rgb 리터럴 금지. 새 색이 필요하면 세 테마(기본/`.dark`/`.cassette`) 모두에 토큰을 추가한다. 액센트 역할 구분: 보라(brand)=상호작용, 앰버(price)=가격, 시맨틱(positive/warning/danger)=상태.
- **폰트는 두 종.** 본문·UI는 `font-sans`(기본이라 명시 불필요), 가격처럼 자릿수 비교가 필요한 숫자는 `font-mono` + `tabular-nums`. 폰트는 Google Fonts CDN 링크로 받는다 — `next/font`는 빌드 때 한글 폰트 수백 개 파일을 내려받다 실패해 빌드를 깨뜨린다.
- **포커스 링은 전역 규칙이 처리한다.** `globals.css`의 `:focus-visible` 규칙이 특이도 0(`:where`)으로 모든 인터랙티브 요소를 덮으므로 컴포넌트마다 focus 스타일을 붙이지 않는다. 커스텀 요소로 포커스를 받게 만들 때만 `tabindex`를 확인한다.
- **모션은 감속 선호를 존중한다.** 전역 `prefers-reduced-motion` 규칙이 이미 전환을 무력화하므로, 개별 애니메이션에서 다시 처리할 필요는 없다.
- 최소 폰트 크기는 `text-xs`(12px). 그보다 작은 임의값은 쓰지 않는다.

## 현재 단계와 도입하지 않는 것

핵심 조회·설정 API 이후 **복수 출처 수집기까지 구현된 상태**다. 현재 수집 출처 목록은 `docs/05-collector-design.md`의 표를 단일 진실 출처로 삼는다. 다음은 의도적으로 보류 중이므로 추가 제안 전 확인:
- Redis (캐시/큐)
- 별도 collector worker 컨테이너
- 인증/멀티유저 (현재 단일 고정 user_id)
- 메시지 큐

수집기는 `collector/` 패키지에 있다. 출처별 하위 패키지(`collector/{source}/`)에 fetch(Client)·parse(Parser)·normalize(CollectService)를 두고, 출처 공통 부분은 `collector/support/`에 있다:

- `SourceCollector` — 출처 하나의 수집 계약(`sourceCode()`, `collect()`). `CollectScheduler`가 구현체를 모두 주입받아 순회하므로 **출처가 늘어도 스케줄러는 바뀌지 않는다.**
- `DealUpsertSupport` — 출처 등록 + `(source, external_id)` 기반 upsert(신규 저장/기존 갱신).
- `CollectedDeal` — 출처별 파싱 결과를 표준화한 형태. 출처마다 없는 정보가 있어 대부분 nullable.
- `HtmlFetcher` — 브라우저 UA 기반 HTML 요청.

**새 출처 추가 = 새 하위 패키지 + `SourceCollector` 구현체.** 파서는 `String html → 결과` 순수 함수로 두고 실제 응답 HTML 픽스처(`src/test/resources/fixtures/`)로 테스트한다.

기존 공통 계약 안에서 출처만 추가할 때의 변경 범위는 **`docs/05`의 현재 출처 표 + 해당 `collector/{source}/` 코드 + 테스트 fixture**다. API·DB·공통 수집 구조·운영 정책이 함께 바뀔 때만 그 계약을 소유한 문서를 추가로 갱신한다.

출처를 붙이기 전에 **robots.txt를 확인한다.** (예: FMKorea는 `User-agent: *`에 `Disallow: /`라 수집 대상이 아니다.)

## 작업 시 유의

- 도메인 로직은 Service에 둔다. Controller는 DTO 매핑·검증·상태 코드만.
- 키워드/출처 필터링은 Service에서 쿼리 조건으로 반영 (`docs/01` 3.2 우선순위 규칙).
- DB 스키마/마이그레이션 변경은 `docs/04` 갱신과 함께.
- API 추가/변경은 `docs/03` 갱신과 함께.
- 필터 우선순위·API 계약 변경은 해당 Service/Controller 테스트와 함께 반영한다.
