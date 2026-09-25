# 03. API 설계 (API Design)

> PickDeal — REST API 초안 및 공통 규약
> 본 문서는 MVP 범위(`docs/01` 3장)에 대응한다. 확장 API는 별도 표기한다.
> 최초 작성: 2026-05-20 · 현재 상태 확인: 2026-09-05
> MVP 표의 API는 현재 모두 구현돼 있다. 3차 API는 방향만 기록하며 아직 구현하지 않는다.

---

## 1. 공통 규약

### 1.1 기본

- Base URL: `/api/v1`
- CORS: 운영 Compose는 `CORS_ALLOWED_ORIGINS`에 지정한 정확한 frontend origin만 허용한다(빈 값은 교차 origin 미허용). 쿠키 credentials는 허용하지 않으며, CORS와 별개로 공개 조회 모드의 쓰기 차단을 유지한다. 설정·배포 절차는 `docs/06`을 따른다.
- 공개 조회용 `compose` 프로필은 `pickdeal.read-only=true`로 공개 쓰기를 `403 / READ_ONLY`로 거부한다. 명시적으로 활성화하고 토큰 인증에 성공한 §8의 수집 전용 POST 두 개만 예외다. 수집기의 내부 DB 저장은 영향을 받지 않는다. 기본 로컬 실행은 기존 쓰기 API를 유지한다.
- 포맷: `application/json; charset=utf-8`
- 인증: 사용자 로그인은 **MVP 없음**(단일 사용자). 내부적으로 고정 `user_id`(예: `1`)를 사용한다. 설정·기존 수동 등록 API는 공개 쓰기를 차단한다(`docs/06`). 원격 수집 API만 별도 Bearer 토큰을 사용하며, 사용자 인증이나 다른 쓰기 권한을 부여하지 않는다.
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
| 401 | 수집기 토큰 누락·불일치 (`UNAUTHORIZED`) |
| 403 | 공개 조회 환경에서 쓰기 요청 (`READ_ONLY`) |
| 404 | 리소스 없음 |
| 409 | 충돌(중복 키워드 등) |
| 405 | 수집 API의 POST 외 메서드 |
| 413 | 수집 요청 본문 1MiB 초과 |
| 500 | 서버 오류 |

### 1.4 페이지네이션 규약

- 방식: **offset 기반**을 기본으로 한다(MVP). 데이터량 증가 시 cursor 기반으로 전환 가능(아래 5장).
- 쿼리 파라미터: `page`(0부터), `size`(기본 20, 최대 100).
- 유효한 정수 범위의 `page`가 마지막 페이지를 넘으면 빈 목록과 `hasNext=false`를 반환한다. 페이지 위치 계산은 정수 오버플로 없이 처리한다.
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
| `shopName` | string[] | N | - | 반복 파라미터로 판매처 복수 선택(OR). 등록된 별칭은 대표 이름으로 비교하며 미등록 이름은 대소문자 포함 정확히 일치. 미지정은 전체 |
| `category` | string | N | - | 카테고리 필터(선택) |
| `q` | string | N | - | 추가 검색어(제목 포함 검색, 선택) |

API는 `latest`와 `discount`를 모두 지원한다. 현재 홈 UI는 수집 데이터의 정가·할인율 확보율이 낮아 `latest`만 사용하며 할인율 정렬 선택 UI는 제공하지 않는다.

> `category`는 딜에 저장된 카테고리 문자열에 대한 단순 옵션 필터다. 수집 normalize 단계에서 정확히 등록된 별칭만 PickDeal 대표 문자열로 통일하며, 미등록 카테고리는 출처 원문을 유지한다. 부분 문자열·유사도 기반 통합은 하지 않는다(`docs/05`).

서버 측 자동 적용 규칙(쿼리 파라미터와 무관, `docs/01` 3.2 준수):

1. 사용자가 **숨김 처리한 출처**의 딜은 제외한다.
2. **제외 키워드**가 제목/요약에 포함된 딜은 제외한다.
3. **관심 키워드**가 1개 이상 등록되어 있으면, 관심 키워드를 포함하는 딜만 노출한다.

출처 표시/`sourceId`/`shopName` 조건을 통과한 Deal을 먼저 `DealGroup` 단위로 묶고, 검색어·카테고리·키워드는 그룹 구성원 중 일치하는 항목이 있는지 확인한다. 대표 카드와 출처 수 역시 이 조건을 통과한 구성원 기준이다. 제외 키워드는 구성원 하나라도 일치하면 그룹 전체를 제외한다. 정렬과 페이지네이션은 그룹화 다음에 적용하므로 `meta.totalElements`는 원본 게시글 수가 아니라 실제 카드 수다.

현재 MVP 구현은 노출 가능한 Deal을 DB에서 조회한 뒤 위 그룹·필터·정렬·페이지 처리를 `DealService` 메모리에서 수행한다. API 계약은 유지하되 운영 데이터에서 병목이 확인되면 DB 쿼리로 이동한다.

> 종료(EXPIRED)/품절(SOLD_OUT) 딜도 목록에 **포함**한다 — 화면이 `status`로 구분해(취소선 등) 표시한다. 조용히 숨기지 않는 것이 핫딜 목록 관례다.

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
      "shopName": "샘플몰",
      "commentCount": 18,
      "thumbnailUrl": "https://.../thumb.jpg",
      "sourceId": 3,
      "sourceName": "샘플커뮤니티",
      "groupId": 51,
      "sourceCount": 2,
      "sourceNames": ["루리웹", "퀘이사존"],
      "postedAt": "2026-05-20T18:10:00+09:00",
      "collectedAt": "2026-05-20T18:12:00+09:00",
      "status": "ACTIVE"
    }
  ],
  "meta": { "page": 0, "size": 20, "totalElements": 137, "totalPages": 7, "hasNext": true }
}
```

> 목록 응답은 카드 렌더링에 필요한 요약 필드만 포함한다(본문/원문 링크 등은 상세에서 제공).
> `shopName`은 출처 게시글이 표시한 판매몰 이름을 표준화하지 않고 저장한 nullable 문자열이다. 제목 관례에서 보완할 수는 있지만 Shop 리소스로 해석하지 않는다.
> `commentCount`는 해당 출처의 원문 게시글에서 마지막으로 확인한 댓글 수다. 출처가 제공하지 않거나 확인할 수 없으면 `null`이며, 여러 출처의 값을 합산하거나 긍정 반응으로 해석하지 않는다.
> `id`, `sourceId`, `sourceName`, `commentCount`와 상품 요약 필드는 그룹 대표 Deal 기준이다. `sourceCount`와 `sourceNames`는 현재 출처 표시/필터 조건을 통과한 구성원 기준이다. `groupId`는 그룹이 없으면 `null`이다. 그룹 상태는 하나라도 활성이라면 `ACTIVE`, 모두 종료된 경우 품절 우선으로 집계한다.

### 2.2 카테고리 목록 조회

```
GET /api/v1/deals/categories
```

노출 중인(표시 출처, 종료/품절 포함) 딜의 카테고리 문자열 목록. 중복 없이 정렬해 반환하며, 프론트 카테고리 필터 바의 데이터 소스다. 카테고리는 확실한 별칭만 최소 정규화하고 나머지는 출처 원문을 유지하므로(2.1 참고) 수집 데이터에 따라 목록이 달라진다.

```json
{ "data": ["PC/하드웨어", "게임/SW", "생활/식품"] }
```

### 2.2.1 쇼핑몰 목록 조회

`GET /api/v1/deals/shops`

DB의 표시 출처 딜(종료/품절 포함)에 저장된 `shopName`에 아래 별칭을 적용한 뒤 중복 없이 정렬해 반환한다. null·공백 이름은 제외한다. 카테고리 목록처럼 현재 검색·키워드·출처 조회 필터에는 종속되지 않는다. 별도 Shop 엔티티는 없다.

| 등록 이름(영문 대소문자 무시) | 대표 이름 |
| --- | --- |
| 카카오쇼핑, 카카오톡딜, 카카오 톡딜 | 카카오쇼핑 |
| G마켓, 지마켓 | 지마켓 |
| 네이버, 네이버쇼핑 | 네이버 |

목록과 `shopName` 필터 양쪽에 같은 규칙을 적용하므로 과거 별칭 URL도 동일한 결과를 조회한다. 중복 별칭을 선택해도 결과는 중복되지 않는다. 미등록 이름은 원문 그대로 정확히 비교하며 부분 일치·유사도·기호 제거는 하지 않는다. `카카오선물하기`·`네이버페이`는 합치지 않는다. 저장된 원문과 딜 응답의 `shopName`, 상품 자동 그룹 판정 규칙은 변경하지 않는다. 기존 데이터에도 조회 시 적용하므로 DB 일괄 갱신은 필요 없다.

응답 예: `{ "data": ["11번가", "네이버", "쿠팡"] }`

선택은 `?shopName=네이버&shopName=쿠팡`처럼 반복 파라미터로 전달한다. 쉼표는 구분자가 아닌 이름의 일부다. 존재하지 않는 이름은 결과 0건이며, 미지정 시 이름 없는 딜도 포함한다.

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
    "shopName": "샘플몰",
    "commentCount": 18,
    "thumbnailUrl": "https://.../thumb.jpg",
    "originalUrl": "https://source.example.com/deal/abc",
    "productUrl": "https://shop.example.com/products/123",
    "sourceId": 3,
    "sourceName": "샘플커뮤니티",
    "groupId": 51,
    "sourceCount": 2,
    "sourceNames": ["루리웹", "퀘이사존"],
    "sourcePosts": [
      {
        "dealId": 1024,
        "sourceId": 3,
        "sourceName": "샘플커뮤니티",
        "originalUrl": "https://source.example.com/deal/abc",
        "productUrl": "https://shop.example.com/products/123",
        "commentCount": 18,
        "postedAt": "2026-05-20T18:10:00+09:00",
        "status": "ACTIVE"
      }
    ],
    "externalId": "abc",
    "postedAt": "2026-05-20T18:10:00+09:00",
    "collectedAt": "2026-05-20T18:12:00+09:00",
    "status": "ACTIVE"
  }
}
```

- `originalUrl`은 수집 출처의 커뮤니티 원문 게시글이고, `productUrl`은 원문이 별도 제공한 HTTP(S) 구매 링크다. 두 값의 의미를 합치거나 서로 대체하지 않는다.
- `productUrl`은 상세 수집에 성공한 신규 Deal에만 있을 수 있는 nullable 값이다. 없으면 화면은 원문 링크만 제공한다.
- `sourcePosts`는 같은 `DealGroup`의 출처별 원본 게시글이며 그룹이 없으면 현재 Deal 한 건을 반환한다. 기존 `/deals/{id}` URL과 상단 상세 필드는 요청한 Deal 기준으로 유지한다.
- 존재하지 않으면 404 + `code: DEAL_NOT_FOUND`.

### 2.4 (선택) 딜 수동 등록 — 내부용

> MVP에서 더미 데이터를 채우기 위한 선택적 내부 API. 공개 UI는 두지 않는다. 시드로 대체 가능(`docs/04`).

```
POST /api/v1/internal/deals
```

요청 본문(주요 필드): `title`, `price`, `originalPrice`, `currency`, `category`, `shopName`, `thumbnailUrl`, `originalUrl`, `productUrl`, `sourceId`, `externalId`, `postedAt`.
- `shopName`은 100자 이하의 선택값, `productUrl`은 HTTP(S) 형식의 2,000자 이하 선택값이다.
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
| POST | `/api/v1/internal/collected-deals/known-external-ids` | 출처 내 기존 ID 일괄 확인 | 원격 수집·기본 비활성 |
| POST | `/api/v1/internal/collected-deals` | 수집 결과 배치 저장/갱신 | 원격 수집·기본 비활성 |
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

## 8. 원격 수집 전용 API

수신 서버와 DB 없는 로컬 수집기의 전송 모드가 구현됐다. 실제 운영 연결은 별도 검증 후 적용한다.
`pickdeal.collector.ingress.enabled=false`가 기본이며 비활성 시 두 경로는 404다.
활성 시 `Authorization: Bearer <collector-token>`을 매 요청에 전송한다. 토큰 누락·오류는
본문 파싱 전에 401, 인증 후 POST 외 메서드는 405다. 끝의 `/`를 붙이지 않는다.
수집기는 서버 간 HTTPS로 호출하며, 토큰은 브라우저/Vercel에 전달하지 않는다.

### 8.1 기존 ID 확인

`POST /api/v1/internal/collected-deals/known-external-ids`

```json
{"sourceCode":"ppomppu","externalIds":["735731","735730"]}
```

```json
{"data":{"knownExternalIds":["735730"],"hasCollectedDeals":true}}
```

- ID 목록은 1~150개, 각 값은 1~200자리 양의 숫자 문자열이다. 응답은 요청 순서로 중복 제거한다.
- 해당 출처의 저장된 ID만 반환한다. 신규 판별을 위한 조회일 뿐 예약/저장 잠금은 아니다.
- `hasCollectedDeals`는 해당 출처에 딜이 하나라도 있는지 나타내므로 향후 bootstrap 판별에 쓸 수 있다.
- 출처가 아직 DB에 없으면 빈 목록/false이며 조회만으로 출처를 생성하지 않는다.

### 8.2 수집 결과 전송

`POST /api/v1/internal/collected-deals`

```json
{
  "sourceCode":"ppomppu",
  "deals":[{
    "externalId":"735731",
    "originalUrl":"https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=735731",
    "shopName":"판매몰",
    "title":"상품 이름 (24,800원/무료)",
    "price":24800,
    "category":"디지털",
    "commentCount":null,
    "thumbnailUrl":null,
    "ended":false,
    "postedAt":"2026-09-20T14:07:27+09:00",
    "productUrl":null
  }]
}
```

```json
{"data":{"received":1,"created":1,"updated":0}}
```

- 출처 코드는 `quasarzone`, `ruliweb`, `ppomppu`만 받는다. 표시명·기본 URL은 서버가 정한다.
  DB에서 비활성인 출처는 400으로 거부한다. 출처별 로컬 수집기 enable 설정과는 별개다.
- 배치는 1~150건. null 항목·중복 externalId·잘못된 항목이 하나라도 있으면 전체 요청을 거부한다.
- 필수: `externalId`, `originalUrl`, 판매처 말머리를 뺀 `title`.
  `ended`는 nullable boolean이다. true는 종료, false는 활성, null/생략은 미확인으로 기존 상태를 유지한다.
  신규 딜의 종료 여부가 미확인이면 기존 모델의 ACTIVE로 등록하며 판매 가능 여부를 보장하지 않는다.
  `shopName`을 붙인 실제 저장 제목은 300자 이하, shopName 100자, category 50자 이하다.
- 가격은 nullable 원화 정수이며 0 이상, 댓글 수도 nullable 정수 0 이상이다.
- 원문 URL은 출처/게시글 ID가 일치하는 HTTPS 주소만 허용한다.
  퀘이사존 `/bbs/qb_saleinfo/views/{externalId}`, 루리웹 `/market/board/1020/read/{externalId}`,
  뽐뿌 `/zboard/view.php?id=ppomppu&no={externalId}`를 사용하고 다른 쿼리/fragment는 제외한다.
- 원문·썸네일 URL 1,000자, 상품 URL 2,000자 이하. 선택 URL은 HTTP(S), 사용자정보 없는 URL만 받는다.
  수신 서버가 이 URL에 접속하지는 않는다. 상품 URL과 원문 URL은 별도로 저장한다.
- postedAt은 nullable ISO-8601 offset 시각이며 없으면 서버 수신 시각을 사용한다.
  카테고리는 기존 정확 일치 정규화를 적용한다.
- 기존 `DealUpsertSupport`와 그룹화 로직을 재사용한다. 재전송은 `(source, externalId)`로 갱신하고
  상세 상품 URL·판매처가 빠졌다고 기존 값을 지우지 않는다. `updated`는 값이 실제 바뀐 수가 아니라
  기존 행 처리 수다. 동일 배치 재전송은 새 행을 만들지 않지만 오래된 서로 다른 배치의 순서 역전은 방지하지 않는다.
- 단일 서버의 수신 배치는 트랜잭션 커밋까지 직렬화하며 DB 유니크 제약도 유지한다.
  같은 출처의 OCI 스케줄러와 로컬 수집기를 동시에 실행하지 않는다. 저장 충돌은 전체 롤백 후 409다.
- 인증 후 본문은 Content-Length 유무와 관계없이 최대 1MiB, 초과 시 413이다.
  메모리 큐·영속 재전송 큐·자동 재시도는 이 API에 포함하지 않는다.
