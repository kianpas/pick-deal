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

## 커밋 메시지

- `feat`, `fix`, `docs`, `refactor`, `test`, `chore` 등 변경 성격에 맞는 영문 타입 뒤에 `: `를 붙이고, 설명은 한글로 작성한다. 예: `feat: 상세 링크 공유 추가`, `fix: 목록 복귀 위치 초기화 수정`.

## 문서 사용

전체 문서를 매번 읽지 말고 작업에 필요한 부분만 확인한다.

- 작업 대상 하위 디렉터리의 `AGENTS.md`도 확인한다. 지침이나 관련 문서가 변경됐거나 현재 대화에 내용이 없으면 다시 읽는다.

- `CONTEXT.md`: 용어·재사용 자산 색인.
- `docs/01~06`: 요구사항·상세 계약. 필터 정책은 `docs/01` §3.2, API는 `docs/03`, DB는 `docs/04`, 수집기는 `docs/05`, 배포는 `docs/06`.
- `docs/roadmap.md`: 남은 작업·보류 항목을 판단할 때 확인. 구현 현황은 코드와 해당 계약 문서를 따른다.
- `docs/adr/`, `docs/notes/`: 결정 근거·과거 조사 기록이 필요할 때 확인.

현재 버전·파일 구조는 코드와 빌드 설정을, 설계 의도는 해당 설계 문서를 기준으로 한다. 둘이 충돌하면 차이를 확인하고 요청 범위에서 해결한다. 구현에 맞추려고 설계 정책을 임의로 바꾸지 않는다.

API·DB·필터 정책 변경은 해당 계약 문서와 테스트를 함께 갱신한다. 관련 없는 문서 정리나 상태 설명의 중복 추가는 하지 않는다.

여러 작업에 걸치는 큰 변경은 목표·진행 상태·검증 결과를 작업별 계획에 남긴다. 단순 수정에 별도 계획 문서를 만들거나 설계 계약을 계획 문서에 복제하지 않는다. 모델별 지침은 공통 규칙을 참조하고, 모델 버전만을 이유로 규칙을 복제하거나 검증을 생략하지 않는다.

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

- 프론트 작업 전 `frontend/AGENTS.md`의 구현·디자인·스킬 적용 기준을 확인한다. UI 스킬은 백엔드·DB·배포 작업에는 사용하지 않는다.
- npm과 `package-lock.json`을 사용한다.
- 목록·상세는 Server Component SSR 우선, 상호작용 부분만 Client Component로 분리한다.
- 백엔드 호출은 `lib/api.ts`, 계약 타입은 `lib/api-types.ts`를 사용한다. `lib/mock-data.ts`·`lib/types.ts`는 데모용이며 신규 API 계약에 사용하지 않는다.
- API base URL은 `NEXT_PUBLIC_API_BASE_URL`. 키워드·출처 설정은 백엔드 DB가 기준이며 localStorage에 저장하지 않는다.

## 수집기

- 구조·출처·요청 제한·DealGroup 정책은 `docs/05-collector-design.md`를 따른다.
- 새 출처는 `collector/{source}/`와 `SourceCollector` 구현체로 추가하고 공통 지원 코드를 재사용한다. Parser는 `String html → 결과` 순수 로직과 실제 HTML fixture로 검증한다.
- 공통 계약이 같으면 출처 추가 시 해당 코드·테스트·fixture와 `docs/05` 출처 설명을 중심으로 갱신한다. 설정·운영 절차가 달라지면 관련 설정 예시와 배포 안내도 갱신한다.
- fixture는 실제 응답에서 파싱 동작을 재현하는 최소 HTML을 저장한다. 전체 원본 응답과 일회성 수집 결과는 Git에서 제외된 로컬 경로에 두며, 필요한 회귀 테스트 fixture까지 제외하지 않는다.
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
| Frontend 동작 변경 | 위 검사 + `npm test` (변경 동작의 회귀 검증 포함) |
| 라우팅·의존성·빌드 설정·SSR/Client 경계 | 위 검사 + `npm run build` |
| Backend 코드 | `./gradlew test` (H2 in-memory, 별도 DB 불필요) |
| DB 스키마·migration | 관련 테스트 + 허용된 PostgreSQL 환경에서 스키마 적용·기동 확인 |
| 수집기·파서 | 관련 fixture 테스트 + 위 실수집 기준 |

UI 변경은 영향받는 화면과 상호작용을 실제로 확인한다. 환경 제약으로 확인하지 못하면 코드 검사 결과와 화면·동작 미검증 항목 및 사유를 구분해 보고한다. 새 테스트는 변경된 동작과 회귀 위험을 검증하는 경우에 추가한다.
