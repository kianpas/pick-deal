# Pick Deal API 설계 초안

## 1. API 설계 원칙

- REST API를 기본으로 한다.
- 모든 응답은 JSON으로 제공한다.
- URL은 리소스 중심으로 설계한다.
- MVP에서는 인증을 두지 않는다.
- 페이지네이션은 핫딜 목록 API부터 적용할 수 있게 설계한다.
- 시간 값은 ISO 8601 문자열을 사용한다.

## 2. 공통 응답 형식

MVP에서는 단순 응답을 우선 사용한다.

```json
{
  "data": {}
}
```

에러 응답 예시는 다음과 같다.

```json
{
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found"
  }
}
```

## 3. 핫딜 API

### 핫딜 목록 조회

```http
GET /api/deals
```

쿼리 파라미터:

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| page | number | N | 페이지 번호, 기본값 0 |
| size | number | N | 페이지 크기, 기본값 20 |
| sourceId | number | N | 특정 출처 필터 |
| q | string | N | 제목/설명 검색어 |

응답 예시:

```json
{
  "data": {
    "items": [
      {
        "id": 1,
        "title": "무선 마우스 특가",
        "price": 19900,
        "shippingFee": 0,
        "source": {
          "id": 1,
          "name": "Example Deals"
        },
        "originalUrl": "https://example.com/deals/1",
        "matchedInterestKeywords": ["마우스"],
        "createdAt": "2026-05-19T10:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

기본 동작:

- 숨김 처리된 출처의 핫딜은 제외한다.
- 제외 키워드가 포함된 핫딜은 제외한다.
- 관심 키워드가 포함된 경우 `matchedInterestKeywords`에 포함한다.
- 최신 등록/수집 시간 기준 내림차순 정렬을 기본값으로 한다.

### 핫딜 상세 조회

```http
GET /api/deals/{dealId}
```

응답 예시:

```json
{
  "data": {
    "id": 1,
    "title": "무선 마우스 특가",
    "description": "수동 등록된 MVP 샘플 핫딜입니다.",
    "price": 19900,
    "shippingFee": 0,
    "source": {
      "id": 1,
      "name": "Example Deals",
      "baseUrl": "https://example.com"
    },
    "originalUrl": "https://example.com/deals/1",
    "matchedInterestKeywords": ["마우스"],
    "createdAt": "2026-05-19T10:00:00+09:00",
    "updatedAt": "2026-05-19T10:00:00+09:00"
  }
}
```

에러:

- `404 NOT_FOUND`: 핫딜이 존재하지 않는 경우

### 핫딜 수동 등록

```http
POST /api/deals
```

요청 예시:

```json
{
  "sourceId": 1,
  "title": "무선 마우스 특가",
  "description": "수동 등록된 MVP 샘플 핫딜입니다.",
  "price": 19900,
  "shippingFee": 0,
  "originalUrl": "https://example.com/deals/1",
  "originalId": "example-1",
  "postedAt": "2026-05-19T10:00:00+09:00"
}
```

응답 예시:

```json
{
  "data": {
    "id": 1,
    "title": "무선 마우스 특가",
    "description": "수동 등록된 MVP 샘플 핫딜입니다.",
    "price": 19900,
    "shippingFee": 0,
    "source": {
      "id": 1,
      "name": "Example Deals",
      "baseUrl": "https://example.com"
    },
    "originalUrl": "https://example.com/deals/1",
    "matchedInterestKeywords": ["마우스"],
    "createdAt": "2026-05-19T10:00:00+09:00",
    "updatedAt": "2026-05-19T10:00:00+09:00"
  }
}
```

## 4. 출처 API

### 출처 목록 조회

```http
GET /api/sources
```

응답 예시:

```json
{
  "data": [
    {
      "id": 1,
      "name": "Example Deals",
      "baseUrl": "https://example.com",
      "description": "개발용 샘플 출처",
      "visible": true,
      "createdAt": "2026-05-19T10:00:00+09:00"
    }
  ]
}
```

### 출처 표시/숨김 설정

```http
PUT /api/sources/{sourceId}/visibility
```

요청 예시:

```json
{
  "visible": false
}
```

응답 예시:

```json
{
  "data": {
    "id": 1,
    "name": "Example Deals",
    "visible": false
  }
}
```

에러:

- `404 NOT_FOUND`: 출처가 존재하지 않는 경우
- `400 BAD_REQUEST`: `visible` 값이 누락된 경우

## 5. 키워드 API

키워드 타입:

- `INTEREST`: 관심 키워드
- `EXCLUDE`: 제외 키워드

### 키워드 목록 조회

```http
GET /api/preferences/keywords
```

쿼리 파라미터:

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| type | string | N | `INTEREST` 또는 `EXCLUDE` |

응답 예시:

```json
{
  "data": [
    {
      "id": 1,
      "type": "INTEREST",
      "value": "마우스",
      "createdAt": "2026-05-19T10:00:00+09:00"
    },
    {
      "id": 2,
      "type": "EXCLUDE",
      "value": "리퍼",
      "createdAt": "2026-05-19T10:05:00+09:00"
    }
  ]
}
```

### 키워드 등록

```http
POST /api/preferences/keywords
```

요청 예시:

```json
{
  "type": "INTEREST",
  "value": "키보드"
}
```

응답 예시:

```json
{
  "data": {
    "id": 3,
    "type": "INTEREST",
    "value": "키보드",
    "createdAt": "2026-05-19T10:10:00+09:00"
  }
}
```

검증:

- `type`은 `INTEREST`, `EXCLUDE` 중 하나여야 한다.
- `value`는 공백 제거 후 1자 이상이어야 한다.
- 동일 타입에 같은 키워드를 중복 등록할 수 없다.

### 키워드 삭제

```http
DELETE /api/preferences/keywords/{keywordId}
```

응답 예시:

```json
{
  "data": {
    "id": 1,
    "deleted": true
  }
}
```

에러:

- `404 NOT_FOUND`: 키워드가 존재하지 않는 경우

## 6. 향후 API 후보

수집기와 AI 요약을 붙일 때 다음 API를 추가할 수 있다.

```http
POST /api/admin/deals
POST /api/admin/collector-runs
GET /api/admin/collector-runs
GET /api/deals/{dealId}/comments-summary
GET /api/deals/{dealId}/price-history
```

관리자 API는 MVP에서 제외한다.
