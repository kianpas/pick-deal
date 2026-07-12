# 03. API 설계 (API Design)

> PickDeal — REST API 초안 및 공통 규약
> 본 문서는 MVP 범위(`docs/01` 3장)에 대응한다. 확장 API는 별도 표기한다.
> 최초 작성: 2026-05-20 · 현재 상태 확인: 2026-07-11
> MVP 표의 API는 현재 모두 구현돼 있다. 3차 API는 방향만 기록하며 아직 구현하지 않는다.

---

## 1. 공통 규약

### 1.1 기본

- Base URL: `/api/v1`
- 포맷: `application/json; charset=utf-8`
- 인증: **MVP 없음**(단일 사용자). 내부적으로 고정 `user_id`(예: `1`)를 사용한다. 향후 인증 도입 시 `Authorization` 헤더를 추가한다.
- 시간 포맷: ISO-8601 문자열. 직렬화 시간대는 **`Asia/Seoul`(KST)**을 사용한다(예: `2026-05-20T20:36:00+09:00`).
- 통화/금액: 금액은 정수(최소 화폐 단위 또는 원 단위)로 표현하고, `currency` 필드(예: `KRW`)를 함께 둔다.

### 1.2 공통 응답 래퍼

성공 응답:

```json
{
  "data": { /* 리소스 또는 목록 */ },
  "meta": { /* 페이지네이션 등 (선택) */ }
}
```

에러 응답:

```json
{
  "error": {
    "code": "DEAL_NOT_FOUND",
    "message": "해당 딜을 찾을 수 없습니다.",
    "details": []
  }
}
```

- `code`: 머신이 분기할 수 있는 상수 문자열(스크리밍 스네이크 케이스).
- `message`: 사용자 노출용 메시지.
- `details`: 필드 검증 오류 등 부가 정보 배열(**선택/예약**). 현재 구현은 사용하지 않으며, 검증 실패 메시지는 `message`에 합쳐 반환한다.

> 구현 메모: 성공/에러는 단일 응답 타입 `ApiResponse{data, meta, error}`에서 비어 있는 필드를 직렬화 시 생략(`@JsonInclude(NON_NULL)`)해 위 두 봉투 형태로 나간다.

### 1.3 HTTP 상태 코드

| 코드 | 사용 상황 |
| --- | --- |
| 200 | 조회/수정 성공 |
| 201 | 생성 성공(키워드 등록 등) |
| 204 | 삭제 성공(본문 없음) |
| 400 | 잘못된 요청(검증 실패) |
| 404 | 리소스 없음 |
| 409 | 충돌(중복 키워드 등) |
| 500 | 서버 오류 |

### 1.4 페이지네이션 규약

- 방식: **offset 기반**을 기본으로 한다(MVP). 데이터량 증가 시 cursor 기반으로 전환 가능(아래 5장).
- 쿼리 파라미터: `page`(0부터), `size`(기본 20, 최대 100).
- 응답 `meta` 형식:

```json
{
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 137,
    "totalPages": 7,
    "hasNext": true
  }
}
```

---

## 2. 딜 (Deals)

### 2.1 핫딜 목록 조회

```
GET /api/v1/deals
```

쿼리 파라미터:

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | int | N | 0 | 페이지 번호(0부터) |
| `size` | int | N | 20 | 페이지 크기(최대 100) |
| `sort` | enum | N | `latest` | `latest`(게시 최신순) \| `discount`(할인율 높은순) |
| `sourceId` | long[] | N | - | 특정 출처만 필터(미지정 시 표시 상태 출처 전체) |
| `category` | string | N | - | 카테고리 필터(선택) |
| `q` | string | N | - | 추가 검색어(제목 포함 검색, 선택) |

> `category`는 딜에 저장된 카테고리 문자열에 대한 단순 옵션 필터다. 출처마다 상이한(또는 없는) 카테고리를 PickDeal 통합 분류로 매핑하는 작업은 **수집기 단계(`docs/05`)로 보류**한다. MVP의 화면 분류 욕구는 키워드/출처 필터로 충족한다.

서버 측 자동 적용 규칙(쿼리 파라미터와 무관, `docs/01` 3.2 준수):

1. 사용자가 **숨김 처리한 출처**의 딜은 제외한다.
2. **제외 키워드**가 제목/요약에 포함된 딜은 제외한다.
3. **관심 키워드**가 1개 이상 등록되어 있으면, 관심 키워드를 포함하는 딜만 노출한다.

응답 예시:

```json
{
  "data": [
    {
      "id": 1024,
      "title": "샘플 SSD 1TB 특가",
      "price": 89000,
      "originalPrice": 129000,
      "discountRate": 31,
      "currency": "KRW",
      "category": "전자제품",
      "thumbnailUrl": "https://.../thumb.jpg",
      "sourceId": 3,
      "sourceName": "샘플커뮤니티",
      "postedAt": "2026-05-20T18:10:00+09:00",
      "collectedAt": "2026-05-20T18:12:00+09:00",
      "status": "ACTIVE"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 137, "totalPages": 7, "hasNext": true }
}
```

> 목록 응답은 카드 렌더링에 필요한 요약 필드만 포함한다(본문/원문 링크 등은 상세에서 제공).

### 2.2 카테고리 목록 조회

```
GET /api/v1/deals/categories
```

노출 중인(ACTIVE + 표시 출처) 딜의 카테고리 문자열 목록. 중복 없이 정렬해 반환하며, 프론트 카테고리 필터 바의 데이터 소스다. 카테고리는 출처가 준 자유 문자열이므로(2.1 참고) 수집 데이터에 따라 목록이 달라진다.

```json
{ "data": ["PC/하드웨어", "게임/SW", "생활/식품"] }
```

### 2.3 핫딜 상세 조회

```
GET /api/v1/deals/{id}
```

응답 예시:

```json
{
  "data": {
    "id": 1024,
    "title": "샘플 SSD 1TB 특가",
    "description": "행사 상세 설명 ...",
    "price": 89000,
    "originalPrice": 129000,
    "discountRate": 31,
    "currency": "KRW",
    "category": "전자제품",
    "thumbnailUrl": "https://.../thumb.jpg",
    "originalUrl": "https://source.example.com/deal/abc",
    "sourceId": 3,
    "sourceName": "샘플커뮤니티",
    "externalId": "abc",
    "postedAt": "2026-05-20T18:10:00+09:00",
    "collectedAt": "2026-05-20T18:12:00+09:00",
    "status": "ACTIVE"
  }
}
```

- 존재하지 않으면 404 + `code: DEAL_NOT_FOUND`.

### 2.4 (선택) 딜 수동 등록 — 내부용

> MVP에서 더미 데이터를 채우기 위한 선택적 내부 API. 공개 UI는 두지 않는다. 시드로 대체 가능(`docs/04`).

```
POST /api/v1/internal/deals
```

요청 본문(주요 필드): `title`, `price`, `originalPrice`, `currency`, `category`, `thumbnailUrl`, `originalUrl`, `sourceId`, `externalId`, `postedAt`.
- `sourceId + externalId` 조합은 유니크. 중복 시 409 + `code: DEAL_DUPLICATED`.

---

## 3. 출처 (Sources)

### 3.1 출처 목록 조회

```
GET /api/v1/sources
```

- 등록된 출처와 현재 사용자의 표시/숨김 상태를 함께 반환한다.

```json
{
  "data": [
    { "id": 1, "name": "샘플커뮤니티", "baseUrl": "https://a.example.com", "visible": true },
    { "id": 2, "name": "딜사이트B",    "baseUrl": "https://b.example.com", "visible": false }
  ]
}
```

### 3.2 출처 표시/숨김 설정

```
PATCH /api/v1/sources/{id}/visibility
```

요청 본문:

```json
{ "visible": false }
```

응답: 200 + 갱신된 출처 항목. 설정은 `source_visibility` 테이블(`user_id` 포함)에 저장한다(`docs/04`).
- 존재하지 않는 출처면 404 + `code: SOURCE_NOT_FOUND`.

---

## 4. 키워드 (Keywords)

> 관심(`INTEREST`) / 제외(`EXCLUDE`) 두 타입을 하나의 리소스로 관리한다.

### 4.1 키워드 목록 조회

```
GET /api/v1/keywords?type=INTEREST   # type 생략 시 전체
```

```json
{
  "data": [
    { "id": 11, "keyword": "SSD",   "type": "INTEREST", "createdAt": "2026-05-20T17:00:00+09:00" },
    { "id": 12, "keyword": "리퍼", "type": "EXCLUDE",  "createdAt": "2026-05-20T17:01:00+09:00" }
  ]
}
```

### 4.2 키워드 등록

```
POST /api/v1/keywords
```

요청 본문:

```json
{ "keyword": "SSD", "type": "INTEREST" }
```

- `type`: `INTEREST` | `EXCLUDE`.
- `keyword`: 공백 trim, 1~50자, 빈 문자열 불가(400 + `code: INVALID_KEYWORD`).
- 동일 `user_id + keyword + type` 중복 시 409 + `code: KEYWORD_DUPLICATED`.
- 성공 시 201 + 생성된 항목.

### 4.3 키워드 삭제

```
DELETE /api/v1/keywords/{id}
```

- 성공 시 204(본문 없음).
- 존재하지 않으면 404 + `code: KEYWORD_NOT_FOUND`.

---

## 5. 확장 시 변경 예정 (참고)

> 아래는 MVP 구현 대상이 아니다. 확장 단계에서 추가/변경한다.

- **cursor 페이지네이션**: 데이터/트래픽 증가 시 `GET /api/v1/deals?cursor=...&size=...`로 전환(`postedAt + id` 기반 키셋).
- **알림 구독 API**(2차): 관심 키워드 알림 on/off, 채널 설정.
- **댓글 요약 조회**(3차): `GET /api/v1/deals/{id}/comment-summary` — collector/AI 결과 노출(`docs/05`).
- **구매 판단 보조**(3차): `GET /api/v1/deals/{id}/buy-advice`.
- **인증**: 멀티유저 전환 시 모든 설정성 API에 `Authorization` 적용, `user_id`를 토큰에서 추출.

---

## 6. 엔드포인트 요약

| 메서드 | 경로 | 설명 | 단계 |
| --- | --- | --- | --- |
| GET | `/api/v1/deals` | 핫딜 목록(필터/정렬/페이지) | MVP |
| GET | `/api/v1/deals/{id}` | 핫딜 상세 | MVP |
| POST | `/api/v1/internal/deals` | 딜 수동 등록(내부용, 선택) | MVP(선택) |
| GET | `/api/v1/sources` | 출처 목록 + 표시 상태 | MVP |
| PATCH | `/api/v1/sources/{id}/visibility` | 출처 표시/숨김 | MVP |
| GET | `/api/v1/keywords` | 키워드 목록 | MVP |
| POST | `/api/v1/keywords` | 키워드 등록 | MVP |
| DELETE | `/api/v1/keywords/{id}` | 키워드 삭제 | MVP |
| GET | `/api/v1/deals/{id}/comment-summary` | 댓글 요약 | 3차 |
| GET | `/api/v1/deals/{id}/buy-advice` | 구매 판단 보조 | 3차 |

---

## 7. 관련 문서

- 필드/제약/인덱스: `docs/04-database-design.md`
- 필터 우선순위 규칙: `docs/01-requirements.md` 3.2
- 화면-API 매핑: `docs/02-architecture.md` 4장
