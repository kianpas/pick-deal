# 04. 데이터베이스 설계 (Database Design)

> PickDeal — DB 테이블 초안 / 인덱스·제약 / PostgreSQL·MySQL 선택 기준 / 시드 전략
> 작성 기준일: 2026-05-20
> 기본 DBMS: **PostgreSQL** (MySQL 선택 시 5장 참고)

---

## 1. 설계 원칙

- MVP는 단일 사용자지만, 설정성 테이블에는 **`user_id`를 미리 둔다**(고정값 사용, 향후 멀티유저 대비).
- 중복 딜 제거(2차)를 위해 **`source_id + external_id` 유니크 제약**을 초기부터 둔다. dedup 로직은 미구현이나 스키마는 대비한다.
- 시간 컬럼은 타임존 포함 타입(PostgreSQL `timestamptz`)을 사용하고 UTC로 저장한다.
- 마이그레이션은 **Flyway**(권장) 또는 Liquibase로 관리한다. 본 문서는 Flyway 네이밍(`V1__init.sql`)을 가정한다.

---

## 2. 테이블 초안

### 2.1 `source` — 출처 사이트

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | PK, auto | 출처 ID |
| `name` | varchar(100) | not null, unique | 출처 표시명 |
| `base_url` | varchar(500) | not null | 출처 기본 URL |
| `code` | varchar(50) | unique | 수집기 식별용 코드(예: `community_a`) |
| `active` | boolean | not null default true | 출처 자체 활성 여부(운영용) |
| `created_at` | timestamptz | not null default now() | 생성 시각 |

### 2.2 `deal` — 핫딜

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | PK, auto | 딜 ID |
| `source_id` | bigint | FK→source.id, not null | 출처 |
| `external_id` | varchar(200) | not null | 출처 내 원본 식별자(dedup용) |
| `title` | varchar(300) | not null | 제목 |
| `description` | text | null | 본문/상세 설명 |
| `price` | bigint | null | 판매가(통화 최소단위/원 단위 정수) |
| `original_price` | bigint | null | 정가 |
| `discount_rate` | int | null | 할인율(%) — 저장 또는 계산값. 정렬용으로 컬럼 유지 권장 |
| `currency` | varchar(8) | not null default 'KRW' | 통화 코드 |
| `category` | varchar(50) | null | 카테고리 |
| `thumbnail_url` | varchar(1000) | null | 썸네일 URL |
| `original_url` | varchar(1000) | not null | 원본 링크 |
| `title_norm_hash` | varchar(64) | null | 정규화 제목 해시(2차 dedup용) |
| `status` | varchar(20) | not null default 'ACTIVE' | `ACTIVE` \| `EXPIRED` \| `SOLD_OUT` |
| `posted_at` | timestamptz | not null | 출처 게시 시각 |
| `collected_at` | timestamptz | not null default now() | 수집/등록 시각 |
| `created_at` | timestamptz | not null default now() | 레코드 생성 시각 |

제약/인덱스:

- `UNIQUE (source_id, external_id)` — 동일 출처의 동일 딜 중복 방지(**dedup 1차 키**).
- `INDEX (posted_at DESC)` — 최신순 정렬.
- `INDEX (discount_rate DESC)` — 할인율 정렬.
- `INDEX (source_id)` — 출처 필터.
- `INDEX (status)` — 활성 딜 필터.
- (2차) `INDEX (title_norm_hash)` — 교차 출처 중복 후보 탐색.
- 제목 포함 검색 최적화는 데이터량 증가 시 PostgreSQL `pg_trgm` GIN 인덱스 도입 고려(MVP는 불필요).

### 2.3 `source_visibility` — 출처 표시/숨김 설정 (사용자별)

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | PK, auto | |
| `user_id` | bigint | not null | MVP 고정값(예: 1) |
| `source_id` | bigint | FK→source.id, not null | 출처 |
| `visible` | boolean | not null default true | 표시 여부 |
| `updated_at` | timestamptz | not null default now() | 갱신 시각 |

제약:

- `UNIQUE (user_id, source_id)` — 사용자×출처당 1행.
- 행이 없으면 기본 `visible = true`로 간주(레코드 없는 출처는 표시).

### 2.4 `keyword` — 관심/제외 키워드 (사용자별)

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | bigint | PK, auto | |
| `user_id` | bigint | not null | MVP 고정값(예: 1) |
| `keyword` | varchar(50) | not null | 키워드(trim) |
| `type` | varchar(10) | not null | `INTEREST` \| `EXCLUDE` |
| `created_at` | timestamptz | not null default now() | 생성 시각 |

제약/인덱스:

- `UNIQUE (user_id, keyword, type)` — 동일 키워드 중복 등록 방지(API 409 대응).
- `INDEX (user_id, type)` — 필터링 시 타입별 조회.

---

## 3. ERD (개념)

```
            ┌──────────────┐
            │   source      │
            │  id (PK)      │
            │  name (UQ)    │
            │  base_url     │
            │  code (UQ)    │
            └──────┬───────┘
                   │ 1
        ┌──────────┼─────────────┐
        │ N                      │ N
┌───────▼────────┐      ┌────────▼───────────┐
│     deal        │      │ source_visibility   │
│  id (PK)        │      │  id (PK)            │
│  source_id (FK) │      │  user_id            │
│  external_id    │      │  source_id (FK)     │
│  ...            │      │  visible            │
│  UQ(source_id,  │      │  UQ(user_id,        │
│     external_id)│      │     source_id)      │
└────────────────┘      └────────────────────┘

┌──────────────────┐
│     keyword       │   (source와 직접 FK 없음, user 설정)
│  id (PK)          │
│  user_id          │
│  keyword          │
│  type             │
│  UQ(user_id,      │
│     keyword,type) │
└──────────────────┘
```

- MVP에는 `user` 테이블을 만들지 않고 `user_id`를 고정값으로 둔다. 멀티유저 전환 시 `user` 테이블을 추가하고 FK를 연결한다.

---

## 4. 목록 조회 쿼리 개념 (MVP)

`GET /api/v1/deals`의 서버 측 필터를 SQL 개념으로 표현하면:

```sql
SELECT d.*
FROM deal d
WHERE d.status = 'ACTIVE'
  -- 1) 숨김 출처 제외
  AND d.source_id NOT IN (
        SELECT sv.source_id FROM source_visibility sv
        WHERE sv.user_id = :userId AND sv.visible = false
      )
  -- 2) 제외 키워드: 제목/설명에 포함되면 제외
  AND NOT EXISTS (
        SELECT 1 FROM keyword k
        WHERE k.user_id = :userId AND k.type = 'EXCLUDE'
          AND (d.title ILIKE '%' || k.keyword || '%'
            OR d.description ILIKE '%' || k.keyword || '%')
      )
  -- 3) 관심 키워드가 1개 이상이면, 하나라도 포함해야 노출
  AND (
        NOT EXISTS (SELECT 1 FROM keyword k WHERE k.user_id = :userId AND k.type = 'INTEREST')
        OR EXISTS (
            SELECT 1 FROM keyword k
            WHERE k.user_id = :userId AND k.type = 'INTEREST'
              AND (d.title ILIKE '%' || k.keyword || '%'
                OR d.description ILIKE '%' || k.keyword || '%')
        )
      )
ORDER BY d.posted_at DESC      -- sort=latest
LIMIT :size OFFSET :page * :size;
```

- `ILIKE`는 PostgreSQL의 대소문자 무시 LIKE. MySQL에서는 기본 collation이 대소문자 무시인 경우 `LIKE`로 동일 동작(5장 참고).
- MVP 데이터량에서는 위 서브쿼리 방식으로 충분하다. 성능 이슈 발생 시 `pg_trgm` 또는 사전 계산 컬럼 도입.

---

## 5. PostgreSQL vs MySQL 선택 기준

> 기본 권장: **PostgreSQL**. 아래는 선택 의사결정을 위한 비교다.

| 항목 | PostgreSQL | MySQL |
| --- | --- | --- |
| 텍스트 검색 | `pg_trgm`, GIN, 전문검색(FTS) 등 강력. 키워드 포함/유사도 검색 확장에 유리 | 기본 FULLTEXT 인덱스 제공(한글은 ngram parser 필요). 상대적으로 제약 |
| JSON | `jsonb` + 인덱싱 강력(수집 원문/메타 저장에 유리) | JSON 타입 지원하나 인덱싱/연산 기능이 상대적으로 약함 |
| 동시성/MVCC | 성숙한 MVCC, 복잡 쿼리에 강함 | 단순 읽기 위주 워크로드에서 가볍고 빠름 |
| 타임존 타입 | `timestamptz` 네이티브 | `TIMESTAMP`(UTC 변환) / `DATETIME` 구분 주의 필요 |
| 확장성(이 프로젝트 맥락) | dedup 해시, 향후 FTS/유사도, jsonb 원문 저장 등 확장 친화 | 운영 친숙도 높고 호스팅/매니지드 옵션 풍부 |
| 레퍼런스/생태계 | 풍부 | 매우 풍부(특히 국내 호스팅) |

선택 가이드:

- **PostgreSQL을 고른다** — 향후 텍스트 검색 고도화, jsonb 기반 수집 원문 저장, dedup/유사도 처리를 고려하면 본 프로젝트 방향과 가장 잘 맞는다.
- **MySQL을 고를 수 있는 경우** — 팀/호스팅 환경이 MySQL에 고정되어 있거나, 매니지드 MySQL 운영 편의가 중요한 경우. 단, 한글 전문검색은 ngram parser 설정이 필요하고 jsonb 수준의 기능은 기대하기 어렵다.

DBMS를 바꿔도 본 스키마는 거의 그대로 사용 가능하다(타입만 매핑: `timestamptz`→`TIMESTAMP`, `text`→`TEXT`, `boolean`→`TINYINT(1)` 등).

---

## 6. 시드 / 더미 데이터 전략 (MVP)

- 실제 크롤링이 없으므로 **시드 데이터로 화면을 채운다.**
- 방법(택1 또는 병행):
  1. **Flyway seed 마이그레이션**: `src/main/resources/db/seed/`에 `R__seed_*.sql`(반복 실행) 또는 환경별 분리 SQL로 출처/딜 더미 삽입.
  2. **JSON import**: `docs`나 `backend/.../seed/*.json`을 애플리케이션 기동 시(로컬 프로파일 한정) 로드해 삽입.
  3. **내부 등록 API**(`POST /api/v1/internal/deals`)로 수동 삽입.
- 시드 데이터는 **로컬/개발 프로파일에서만** 적재하고, 운영 프로파일에서는 적재하지 않도록 분리한다.
- 시드 딜은 `source_id + external_id` 유니크를 만족하도록 구성한다(중복 삽입 방지 확인용으로도 유용).

---

## 7. 향후 확장 시 스키마 변화 (참고)

- **사용자 추가**: `user` 테이블 신설, 기존 `user_id`에 FK 연결.
- **댓글/요약(3차)**: `deal_comment`(원문), `deal_comment_summary`(AI 요약 결과, 모델/생성시각 메타 포함) 테이블 추가. 상세 `docs/05`.
- **알림(2차)**: `keyword_alert`(구독 설정), `notification`(발송 이력).
- **수집 메타(2차)**: `collect_job`/`collect_log` 또는 jsonb 원문 컬럼/테이블 추가.
- **중복 그룹(2차)**: `deal_group`(대표 딜 + 묶인 딜) 또는 `deal.group_id` 컬럼 추가.

---

## 8. 관련 문서

- API 필드/응답: `docs/03-api-design.md`
- 필터 우선순위 규칙: `docs/01-requirements.md` 3.2
- 수집기/dedup/AI: `docs/05-collector-design.md`
