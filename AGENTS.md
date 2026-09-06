# AGENTS.md

PickDeal은 단일 사용자 핫딜 수집·조회 MVP다.

## Stack

- Frontend: `frontend/` — Next.js App Router, TypeScript strict, Tailwind CSS, npm
- Backend: `backend/` — Spring Boot, Java 17, Gradle, JPA
- Database: PostgreSQL
- 정확한 라이브러리 버전은 `frontend/package.json`과 `backend/build.gradle`을 따른다.

## 작업 범위

- 설명·검토·진단·계획 요청은 조회와 결과 보고까지, 구현·수정 요청은 범위 내 변경과 관련 검증까지 수행한다.
- 허용된 로컬 작업은 진행하되, 배포·운영 데이터 변경·파괴적 작업·비용 발생·범위 확대는 별도 승인이 없다면 확인한다. 기존 사용자 변경을 보존한다.
- 비밀정보를 코드·문서·로그에 남기지 않는다. 인증 없는 쓰기 API를 공개 인터넷에 노출하지 않는다.
- 결과는 변경 요점, 검증 결과, 남은 문제를 간결하게 보고한다. 실행하지 못한 검증을 통과로 표현하지 않는다.

## 문서 사용

전체 문서를 매번 읽지 말고 작업에 필요한 부분만 확인한다.

- `CONTEXT.md`: 용어·재사용 자산 색인.
- `docs/01~06`: 요구사항·상세 계약. 필터 정책은 `docs/01` §3.2, API는 `docs/03`, DB는 `docs/04`, 수집기는 `docs/05`, 배포는 `docs/06`.
- `docs/roadmap.md`: 현재 상태·다음 작업을 판단할 때 확인.
- `docs/adr/`, `docs/notes/`: 결정 근거·과거 조사 기록이 필요할 때 확인.

현재 버전·파일 구조는 코드와 빌드 설정을, 설계 의도는 해당 설계 문서를 기준으로 한다. 둘이 충돌하면 차이를 확인하고 요청 범위에서 해결한다. 구현에 맞추려고 설계 정책을 임의로 바꾸지 않는다.

API·DB·필터 정책 변경은 해당 계약 문서와 테스트를 함께 갱신한다. 관련 없는 문서 정리나 상태 설명의 중복 추가는 하지 않는다.

## 설계 범위

- 현재 요구사항과 데이터 정합성·API 계약·되돌리기 어려운 경계에 집중한다. 기존 자산을 재사용하고, 필요 없는 문서·추상화는 추가하지 않는다.
- 미확정 미래 기능은 방향만 짧게 기록한다. Redis·메시지 큐·별도 worker·인증/멀티유저는 보류 중이며 필요성이 확인되거나 사용자가 요청할 때 재검토한다. 기존 `user_id`는 유지한다.
- 개발·운영 DB는 PostgreSQL을 기준으로 한다. H2 테스트 통과만으로 PostgreSQL 호환성을 판단하지 않는다.

## Backend

- `com.pickdeal.{domain}/` 아래 `api/`, `application/`, `domain/`, `dto/` 구조를 따른다. 도메인 패키지명은 API 리소스명과 맞춘다.
- 트랜잭션·도메인 로직은 Service, Controller는 DTO 매핑·검증·상태 코드 처리를 맡는다.
- API prefix는 `/api/v1`. 응답은 `ApiResponse<T>`, 페이지 정보는 `PageMetaResponse`를 사용한다.
- 에러는 `BusinessException`·`ErrorCode`·`GlobalExceptionHandler` 체계를 재사용한다.
- 필터·그룹·정렬·페이지 처리의 DB 이관은 실제 병목이 확인되거나 명시적으로 요청된 경우에 진행한다.

## Frontend

- npm과 `package-lock.json`을 사용한다.
- 목록·상세는 Server Component SSR 우선, 상호작용 부분만 Client Component로 분리한다.
- 백엔드 호출은 `lib/api.ts`, 계약 타입은 `lib/api-types.ts`를 사용한다. `lib/mock-data.ts`·`lib/types.ts`는 데모용이며 신규 API 계약에 사용하지 않는다.
- API base URL은 `NEXT_PUBLIC_API_BASE_URL`. 키워드·출처 설정은 백엔드 DB가 기준이며 localStorage에 저장하지 않는다.

### 디자인

- 색·폰트 토큰은 `frontend/app/globals.css`가 기준이다. 컴포넌트의 hex/rgb 리터럴을 피하고 새 색은 기본·`.dark`·`.cassette` 테마에 모두 정의한다.
- brand는 상호작용, price는 가격, positive/warning/danger는 상태에 사용한다.
- 본문은 `font-sans`, 자릿수 비교용 숫자는 `font-mono tabular-nums`. 최소 크기는 `text-xs`.
- 폰트는 현재 Google Fonts CDN 방식을 유지한다. `next/font` 전환은 기존 빌드 다운로드 실패 문제가 해결되는지 검증할 때만 한다.
- 전역 focus-visible·reduced-motion 규칙을 재사용한다. 커스텀 요소·애니메이션은 키보드 접근과 감속 설정 적용 여부를 확인한다.

## 수집기

- 구조·출처·요청 제한·DealGroup 정책은 `docs/05-collector-design.md`를 따른다.
- 새 출처는 `collector/{source}/`와 `SourceCollector` 구현체로 추가하고 공통 지원 코드를 재사용한다. Parser는 `String html → 결과` 순수 로직과 실제 HTML fixture로 검증한다.
- 공통 계약이 같으면 출처 추가 시 해당 코드·테스트·fixture와 `docs/05` 출처 표만 갱신한다.
- 새 출처의 robots.txt와 접근 정책을 확인하고 차단 우회는 하지 않는다.
- 외부 응답에 의존하는 변경은 fixture 검증 후 허용된 로컬·개발 환경에서 요청 상한 내 1회 수집으로 확인한다. 운영 DB를 사용하거나 자동 스케줄러와 중복 실행하지 않는다. 접근·환경 제한이 있으면 우회하지 말고 미검증 사유를 보고한다.

## 실행·검증

명령은 각 디렉터리에서 실행한다.

- Backend 실행: `backend/`에서 `./gradlew bootRun`. 로컬 PostgreSQL `pickdeal` DB가 필요하며 접속 정보는 `DB_USERNAME`·`DB_PASSWORD`로 설정한다.
- Frontend 실행: `frontend/`에서 `npm install` 후 `npm run dev`.

변경 범위에 맞춰 검증하고, 새 변경·실패·미해결 문제가 없다면 통과한 검사를 반복하지 않는다.

| 변경 | 검증 |
| --- | --- |
| 모든 변경 | `git diff --check` |
| Frontend 코드 | `npm run lint`, `npm run typecheck` |
| 라우팅·의존성·빌드 설정·SSR/Client 경계 | 위 검사 + `npm run build` |
| Backend 코드 | `./gradlew test` (H2 in-memory, 별도 DB 불필요) |
| DB 스키마·migration | 관련 테스트 + 허용된 PostgreSQL 환경에서 스키마 적용·기동 확인 |
| 수집기·파서 | 관련 fixture 테스트 + 위 실수집 기준 |

UI 변경은 가능하면 관련 화면·상호작용도 확인한다. 새 테스트는 변경된 동작과 회귀 위험을 검증하는 경우에 추가한다.
