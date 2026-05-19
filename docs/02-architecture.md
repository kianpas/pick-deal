# Pick Deal 아키텍처 초안

## 1. 전체 구조

Pick Deal은 하나의 저장소 안에서 프론트엔드와 백엔드를 분리하는 모노레포 구조로 시작한다.

```text
pick-deal/
  frontend/
    Next.js App Router
    TypeScript
    Tailwind CSS
  backend/
    Spring Boot
    REST API
    DB 연동
  docs/
    01-requirements.md
    02-architecture.md
    03-api-design.md
    04-database-design.md
    05-collector-design.md
    06-deployment.md
```

## 2. 런타임 아키텍처

```text
Browser
  -> Next.js Frontend
    -> Spring Boot REST API
      -> Database

Future:
  Collector Worker
    -> External Communities / Deal Sites
    -> Database

  AI Summary Worker
    -> Collected Comments
    -> Summary Tables
```

## 3. 컴포넌트 책임

### Frontend

- 핫딜 목록 화면 제공
- 핫딜 상세 화면 제공
- 출처 표시/숨김 설정 화면 제공
- 관심/제외 키워드 설정 화면 제공
- REST API 호출 및 UI 상태 관리
- 서버 상태 캐싱 전략은 추후 React Query 또는 Next.js fetch 캐싱으로 결정

### Backend

- 핫딜 조회 REST API 제공
- 출처 사이트 조회 및 표시 상태 변경 API 제공
- 관심/제외 키워드 관리 API 제공
- DB 영속화 처리
- MVP 더미 데이터 seed 또는 수동 등록 데이터 제공
- 추후 수집기와 AI 요약 처리의 도메인 모델 기반 제공

### Database

- 핫딜 데이터 저장
- 출처 사이트 정보 저장
- 출처 표시/숨김 설정 저장
- 관심 키워드와 제외 키워드 저장
- 추후 수집 이력, 댓글, AI 요약, 가격 변경 이력 저장

## 4. 프론트엔드 화면 구조

```text
/
  핫딜 목록

/deals/[dealId]
  핫딜 상세

/sources
  출처 사이트 설정

/keywords
  관심 키워드 / 제외 키워드 설정
```

### 공통 레이아웃

- 상단 내비게이션
  - 핫딜
  - 출처
  - 키워드
- 본문 영역
- 모바일 기준 단일 컬럼 우선
- 데스크톱에서는 목록과 필터 영역을 확장 가능

### 핫딜 목록 화면

표시 항목:

- 제목
- 가격
- 배송비
- 출처명
- 등록/수집 시간
- 관심 키워드 매칭 여부
- 원문 링크

가능한 UI 기능:

- 카드 또는 리스트 형태 목록
- 출처별 뱃지
- 제외 키워드로 숨겨진 항목은 기본 미노출
- 상세 페이지 이동

### 핫딜 상세 화면

표시 항목:

- 제목
- 가격
- 배송비
- 출처
- 원문 URL
- 상세 설명
- 등록/수집 시간
- 원문 이동 버튼

추후 표시 항목:

- 댓글 요약
- 가격 변경 이력
- 유사 핫딜
- 품절 여부

### 출처 설정 화면

표시 항목:

- 출처명
- 도메인 또는 기본 URL
- 설명
- 표시 여부 토글

주요 동작:

- 출처 목록 조회
- 출처 표시/숨김 변경

### 키워드 설정 화면

표시 항목:

- 관심 키워드 목록
- 제외 키워드 목록

주요 동작:

- 관심 키워드 추가
- 관심 키워드 삭제
- 제외 키워드 추가
- 제외 키워드 삭제

## 5. 백엔드 패키지 구조

초기 패키지 구조 예시는 다음과 같다.

```text
com.pickdeal
  PickDealApplication

  deal
    DealController
    DealService
    DealRepository
    Deal
    DealResponse
    DealDetailResponse

  source
    SourceController
    SourceService
    SourceRepository
    Source
    SourceResponse
    UpdateSourceVisibilityRequest

  preference
    PreferenceController
    PreferenceService
    PreferenceKeywordRepository
    PreferenceKeyword
    KeywordType
    KeywordResponse
    CreateKeywordRequest

  common
    ApiResponse
    ErrorResponse
    GlobalExceptionHandler

  config
    DatabaseConfig
    CorsConfig
```

## 6. 모듈 분리 기준

### deal

핫딜 목록과 상세 조회를 담당한다. 출처와 키워드 설정을 이용해 조회 조건을 구성할 수 있지만, 설정 변경 자체는 담당하지 않는다.

### source

출처 사이트의 메타데이터와 표시/숨김 상태를 담당한다.

### preference

관심 키워드와 제외 키워드의 등록, 조회, 삭제를 담당한다.

### common

공통 응답 포맷, 예외 처리, 공통 유틸리티를 둔다.

## 7. 데이터 흐름

### 핫딜 목록 조회

1. 프론트엔드가 `GET /api/deals`를 호출한다.
2. 백엔드는 표시 상태가 활성화된 출처 목록을 확인한다.
3. 백엔드는 제외 키워드를 적용한다.
4. 백엔드는 관심 키워드 매칭 정보를 계산한다.
5. 백엔드는 정렬된 핫딜 목록을 반환한다.

### 출처 표시 상태 변경

1. 프론트엔드가 `PUT /api/sources/{sourceId}/visibility`를 호출한다.
2. 백엔드는 해당 출처의 표시 여부를 변경한다.
3. 이후 핫딜 목록 조회에 변경된 상태가 반영된다.

### 키워드 등록/삭제

1. 프론트엔드가 키워드 API를 호출한다.
2. 백엔드는 키워드 타입과 값을 검증한다.
3. 백엔드는 중복 여부를 확인하고 저장 또는 삭제한다.

## 8. 확장 방향

- 수집기는 백엔드 내부 스케줄러로 시작하고, 트래픽과 복잡도가 늘면 별도 worker로 분리한다.
- AI 댓글 요약은 댓글 수집 테이블이 안정된 뒤 비동기 처리로 추가한다.
- 개인용 단일 사용자 구조에서 다중 사용자 구조로 확장할 경우 모든 설정 테이블에 `user_id`를 추가한다.
- 검색 품질이 중요해지면 DB 단순 검색에서 전문 검색 엔진 또는 벡터 검색으로 분리한다.
