# PickDeal 개선 로드맵 및 AI 작업 가이드

> 최초 작성: 2026-08-20 · 최근 갱신: 2026-09-01 (상품 URL 백필·가격 이력 제외 결정 반영)
> 대상 저장소: `kianpas/pick-deal`  
> 목적: Codex / Claude Code 등 AI coding agent가 현재 프로젝트 상태와 설계 의도를 한 번에 이해하고, 과설계 없이 다음 작업을 순차적으로 진행하기 위한 기준 문서

---

## 0. 이 문서의 역할

이 문서는 PickDeal의 기존 `docs/01~06`, `AGENTS.md`, `CONTEXT.md`, ADR을 대체하지 않는다.

역할은 다음과 같다.

1. 현재 구현 상태와 최근 논의를 한곳에 요약한다.
2. 앞으로 무엇을 어떤 순서로 개선할지 우선순위를 제공한다.
3. 아직 구현할 필요가 없는 기술을 명확히 구분한다.
4. Codex / Claude Code가 한 번에 너무 많은 구조 변경을 하지 않도록 작업 단위를 제시한다.
5. 수집 정책, 이미지 처리, 중복 딜 처리 같은 운영 원칙을 명확히 한다.

AI agent는 작업 전에 반드시 현재 코드를 먼저 확인해야 한다.  
이 문서와 코드가 충돌하면 **실제 코드가 현재 상태의 진실 출처**이며, 설계 의도가 바뀐 것이 아니라 문서가 오래된 경우 관련 문서를 함께 갱신한다.

## 0.1 이 문서의 위치

이 문서는 저장소 안(`docs/roadmap.md`)에 두고 `AGENTS.md`에서 참조한다. 로컬 파일로만 두면 AI agent가 읽지 못해 작업이 이 문서 없이 진행된다.

`docs/01~06`(설계 명세)이나 `docs/notes/`(특정 시점의 조사·리뷰 기록)와 달리, 이 문서는 **작업이 진행되면 계속 갱신하는 살아 있는 문서**다. 작업을 완료하면 2장 현재 상태와 18장 작업 목록을 함께 갱신한다.

---

# 1. 프로젝트 방향

PickDeal은 단순한 핫딜 크롤러나 가격비교 사이트가 아니다.

핵심 방향은 다음과 같다.

```text
여러 핫딜 출처
    ↓
수집 / 정규화
    ↓
동일 상품·동일 딜 통합
    ↓
사용자 관심 기준 필터링
    ↓
가격·출처·반응을 한 화면에 제공
    ↓
향후 알림 / 댓글 요약 / 구매 판단 보조
```

다나와처럼 상품을 중심으로 가격비교를 하는 서비스와 일부 구조는 비슷하지만, PickDeal의 핵심 데이터는 **여러 커뮤니티에 게시된 핫딜과 그 반응**이다.

차별점은 다음과 같다.

- 여러 핫딜 출처를 한 화면에서 본다.
- 사용자가 보고 싶은 출처를 선택한다.
- 관심/제외 키워드로 개인화한다.
- 같은 상품이 여러 출처에 올라오면 목록에서는 하나로 묶는다.
- 상세에서는 해당 상품이 어느 출처들에 올라왔는지 보여준다.
- 이후 댓글 반응, 알림, AI 구매 판단 보조로 확장한다.

따라서 장기적으로 중요한 개념은 단순 `Deal` 개수 증가가 아니라 다음 두 축이다.

```text
Source Post
- 어느 커뮤니티에 어떤 글이 올라왔는가

Product / Deal Group
- 여러 글이 실제로 같은 상품 또는 같은 딜인가
```

---

# 2. 현재 구현 상태

2026-08-26 기준 확인된 상태다. **작업 1~9 완료.**

## 2.0 실측 수치 (2026-08-26 로컬 1회 수집)

서술만으로는 기능이 실제로 동작하는지 알 수 없다. 작업을 완료할 때마다 이 표를 갱신한다.

| 항목 | 퀘이사존 | 루리웹 |
| --- | --- | --- |
| 1회 수집 건수 | 29~30 | 28 |
| 가격 확보율 | 29/29 | 3/28 (제목 관례가 제각각이라 의도적으로 보수적) |
| 판매몰 확보율 | 29/29 | 28/28 |
| 상품 URL 확보율 | 2/29 | 3/28 |
| 썸네일 확보율 | 29/29 | 0/28 (목록에 썸네일 없음) |
| 댓글 수 확보율 | 27/29 | 25/28 |
| 교차 출처 그룹 | **0건** | |

> 상품 URL 확보율이 낮은 이유: 상세 보강이 **신규 딜에만** 적용되고 `max-detail-requests`(주기·출처당 3건)로 제한된다. 그룹이 0건인 것은 판정 규칙이 아니라 **상품 URL 커버리지**가 제약이기 때문이다.
>
> 이 수치들은 수집 회차마다 로그로도 남는다: `수집 필드 확보율 [quasarzone]: price=29/29 ... productUrl=2/29`



## Backend

- Spring Boot 4.0.6
- Java 17
- JPA
- PostgreSQL
- 테스트는 H2 in-memory
- `/api/v1/*`
- 도메인 중심 + `api/application/domain/dto` 하위 구조
- `ApiResponse<T>` 공통 응답
- `BusinessException + ErrorCode`
- 현재 단일 사용자, `user_id` 고정 사용
- 인증 미도입
- Redis 미도입
- 별도 collector worker 미도입

## Frontend

- Next.js 16 App Router
- TypeScript strict
- Tailwind CSS
- 목록 / 상세 / 키워드 설정 구현
- 출처 표시/숨김은 사이드바에서 API와 연동
- 백엔드 계약 타입은 `frontend/lib/api-types.ts`가 기준
- SSR 우선, 상호작용이 필요한 부분만 Client Component

## Collector

현재 두 출처가 구현되어 있다.

- Quasarzone
- Ruliweb

공통 구조:

```text
CollectScheduler
    ↓
SourceCollector 구현체
    ↓
출처별 Client / Parser / CollectService
    ↓
CollectedDeal
    ↓
DealUpsertSupport
    ↓
DB
```

현재 동일 출처의 동일 게시글은 다음으로 중복 방지한다.

```text
UNIQUE(source_id, external_id)
```

교차 출처 dedup은 작업 8에서 도입했다(`DealGroupingService`, `deal_group`). 원본 Deal은 삭제하지 않고 그룹으로 연결만 한다. 다만 **판정 근거가 되는 상품 URL 확보율이 낮아 아직 실제로 묶인 그룹은 없다**(2.0 참고).

현재 `Deal`에는 실제 판매 쇼핑몰을 별도 엔티티로 두지 않고 필드로만 갖는다.

- `source` = 핫딜 글이 올라온 커뮤니티
- `originalUrl` = 커뮤니티 원문 글
- `thumbnailUrl` = 원격 썸네일 URL
- `shopName` / `productUrl` = 작업 7에서 nullable 필드로 도입 (별도 Shop 엔티티는 아직 없음)
- `commentCount` = 작업 2에서 도입
- `titleNormHash` = dedup 후보 탐색용

---

# 3. 기본 설계 원칙

## 3.1 과설계하지 않는다

현재 프로젝트 규모에서 다음은 미리 넣지 않는다.

- Kafka
- RabbitMQ
- 별도 메시지 브로커
- Kubernetes 전제 아키텍처
- 다중 worker HA
- 복잡한 이벤트 기반 구조
- 처음부터 완성형 Product Catalog
- AI embedding 기반 dedup
- 모든 사이트를 포괄하는 범용 crawler framework

필요가 생겼을 때 단계적으로 추가한다.

## 3.2 나중에 붙이면 비싼 것만 먼저 준비한다

지금 신경 써야 하는 것:

- 데이터 정합성
- 식별자
- 출처/판매처 개념 분리
- dedup을 위한 데이터 확보
- 공개 API 계약
- 마이그레이션 가능한 DB 구조
- 수집 정책

나중에 비교적 쉽게 붙일 수 있는 것:

- Redis
- worker 분리
- 알림 채널 추가
- AI 모델 교체
- cache
- 관리자 UI

## 3.3 외부 데이터는 완벽하게 파싱하려 하지 않는다

외부 사이트마다 제공 정보가 다르다.

예:

- 어떤 사이트는 가격을 구조화해서 제공
- 어떤 사이트는 제목에 가격이 포함
- 어떤 사이트는 썸네일이 없음
- 어떤 사이트는 판매처 링크가 없음
- 어떤 사이트는 댓글 수만 목록에서 확인 가능

원칙:

> 없는 값은 `null`로 둔다. 애매한 값을 추측해서 잘못 저장하는 것보다 값이 없는 것이 낫다.

## 3.4 외부 사이트 구조는 바뀐다 — 확보율로 감시한다

파서가 의존하는 선택자는 사이트가 구조를 바꾸면 **예외 없이 조용히 `null`이 된다.** 테스트는 저장된 fixture로 돌기 때문에 계속 통과하고, 아무도 모르는 채로 기능이 0%가 된다.

실제 사례(2026-08-26):

```text
QuasarzoneDetailParser가 상세 표의 `<th>링크</th>`를 찾도록 구현
→ 사이트가 그 행을 없앰 (표본 7건 전부, 조사 당시 표본 글까지)
→ product_url 0건. 상세 요청 3건/주기를 쓰면서 아무것도 얻지 못함
→ 단위 테스트는 전부 통과 (fixture에는 옛 구조가 남아 있으므로)
```

따라서:

- 수집 회차마다 **필드별 확보율을 로그로 남긴다**(`DealUpsertSupport`).
- 파서를 새로 만들거나 고칠 때는 **fixture만 믿지 말고 실제 페이지를 여러 건 확인**한다.
- 확보율이 0이거나 급락하면 파서 문제로 먼저 의심한다.

---

# 4. 수집 정책

## 4.1 robots.txt

새 출처 추가 전에 `robots.txt`를 확인한다.

PickDeal의 기본 정책:

```text
robots.txt에서 수집 경로가 명시적으로 차단
→ 정식 수집 대상에서 제외
```

`robots.txt`는 인증/접근통제 그 자체는 아니지만, 공개 서비스로 운영하는 PickDeal은 이를 존중하는 방향으로 간다.

현재 예:

```text
FMKorea
User-agent: *
Disallow: /

→ PickDeal 직접 수집 대상에서 제외
```

## 4.2 anti-bot 우회를 정식 기능으로 만들지 않는다

다음 방식은 기술적으로 가능할 수 있으나 PickDeal의 기본 수집 전략으로 사용하지 않는다.

- robots 제한을 피하기 위한 Playwright/Selenium 도입
- User-Agent 순환으로 차단 우회
- 모바일/미러 URL을 차단 우회 목적으로 순차 시도
- aggregator를 proxy처럼 이용해 원 출처 데이터를 우회 획득

Playwright는 **JavaScript 렌더링이 반드시 필요한 허용된 사이트**처럼 합당한 이유가 있을 때만 검토한다.

## 4.3 다른 핫딜 종합사이트 활용

다른 집계 사이트를 다음 목적으로 사용하지 않는다.

```text
원 사이트 직접 수집 불가
→ 종합사이트 HTML을 긁어서 원 사이트 데이터 확보
```

이 방식은 proxy/우회 구조가 되므로 피한다.

다만 종합사이트가 공식적으로 다음을 제공한다면 별도 판단할 수 있다.

- 공개 API
- RSS
- 데이터 Feed
- 재사용이 허용된 데이터 제공 방식

필요해지면 `Source`와 실제 데이터를 가져온 `Provider`를 분리할 수 있지만, 현재는 이 구조를 미리 구현하지 않는다.

## 4.4 이미지 정책

현재 방식을 유지한다.

```text
이미지 파일 다운로드
→ PickDeal 서버/S3 저장
```

가 아니라,

```text
thumbnail URL만 DB 저장
→ 프론트에서 원격 이미지 표시
```

방식을 기본으로 한다.

현재 단계에서 이미지를 자체 스토리지에 복제하여 영구 보관하지 않는다.

추후 공식 쇼핑몰 API나 상품 Feed가 이미지 사용 조건과 함께 제공되면 대표 상품 이미지 정책을 다시 검토한다.

---

# 5. 개선 우선순위

전체 우선순위는 다음과 같다.

```text
Phase 0. 문서 현재화
Phase 1. 저비용 데이터 개선
Phase 2. 수집기 운영 개선
Phase 3. 판매처 / 상품 식별 데이터 확보
Phase 4. 교차 출처 dedup + DealGroup
Phase 5. 개인화 UX 강화
Phase 6. 가격 이력 (제외 결정)
Phase 7. 실제 상시 운영 / 배포 기반
Phase 8. 알림 + worker 처리
Phase 9. AI 기능
```

각 단계는 하나의 거대한 PR로 만들지 않는다.

## 5.1 모든 작업의 공통 완료 조건

> 이 절은 2026-08-26 리뷰에서 추가됐다. 작업 2~9가 **단위 테스트를 모두 통과한 채로 실데이터에서 0%로 동작하지 않는** 문제가 있었고, 원인은 완료 조건이 fixture 테스트까지만 요구한 것이었다.

데이터를 수집·저장·가공하는 작업은 다음을 모두 만족해야 완료다.

1. **fixture 기반 단위 테스트** (기존과 동일)
2. **실제 1회 수집 후 확보율 확인** — 새로 만든 필드/기능이 실데이터에서 몇 %나 채워지는지 수치로 본다
3. **수치가 예상보다 낮으면 원인을 기록한다** — 그대로 둘지(출처가 정보를 안 주는 경우) 고칠지(파서 문제) 판단해 PR 본문이나 `docs/notes/`에 남긴다
4. 관련 문서 갱신(2장 실측 수치 표 포함)

```text
"테스트 통과 = 동작"이 아니다.
fixture는 과거의 사이트 구조이고, 실데이터가 현재의 사이트 구조다.
```

측정은 배포를 기다릴 필요가 없다. **로컬에서 앱을 한 번 띄우면 된다**(21장 지표 중 확보율 계열은 전부 이렇게 측정 가능하다).

---

# 6. Phase 0 — 문서 현재화

## 목적

현재 코드보다 오래된 문서 표현을 정리한다.

특히 일부 문서에 다음 표현이 남아 있을 가능성이 있다.

```text
Quasarzone 한 출처 구현
```

실제 상태는:

```text
Quasarzone + Ruliweb
```

이다.

## 확인 대상

- `AGENTS.md`
- `CONTEXT.md`
- `docs/01-requirements.md`
- `docs/02-architecture.md`
- `docs/04-database-design.md`
- `docs/05-collector-design.md`
- `docs/06-deployment.md`

## 작업 원칙

새로운 설계를 추가하기보다 **현재 코드 상태와 맞지 않는 status 문구만 우선 정정**한다.

---

# 7. Phase 1 — 저비용 데이터 개선

## 7.1 댓글 수 수집

Quasarzone과 Ruliweb 모두 목록 HTML에서 댓글 수를 확인할 수 있는 것으로 이미 조사되어 있다.

예:

```text
Quasarzone
ctn-count

Ruliweb
num_reply
```

추가 상세 페이지 요청 없이 목록 파싱 단계에서 확보할 수 있다.

### 목표

`CollectedDeal → Deal → DealSummaryResponse → DealCard`까지 `commentCount`를 연결한다.

### 활용

- 카드에 댓글 수 표시
- 향후 인기순 정렬
- AI 댓글 요약 실행 여부 판단
- 딜 반응 정도의 간단한 신호

### 완료 조건

- 두 출처 parser fixture test 추가/수정
- 댓글이 없으면 0 또는 null 정책을 명확히 결정
- API 계약 문서 갱신
- 프론트 렌더 확인
- **실측**: 1회 수집 후 댓글 수 확보율 확인 (5.1 공통 완료 조건)

> 상태: **완료** (2026-08-26 실측 퀘이사존 27/29, 루리웹 25/28)

---

## 7.2 카테고리 최소 통합

출처마다 의미가 같은 카테고리 문자열이 다르다.

예:

```text
게임/SW
게임S/W

상품권/쿠폰
상품권
```

완벽한 taxonomy를 만들지 않는다.

### 추천 방식

각 출처의 normalize 단계에서 **명백히 같은 것만** 통합한다.

```text
source category
    ↓
known mapping?
 ├─ yes → PickDeal 표준 category
 └─ no  → 원문 값 유지
```

### 피할 것

- 모든 상품 카테고리를 처음부터 새로 설계
- DB lookup table + 관리자 UI까지 확장
- AI 분류를 먼저 도입

---

# 8. Phase 2 — 수집기 운영 개선

이 단계는 `idoyo7/hotdeal` 레포에서 참고할 가치가 있는 부분을 PickDeal 규모에 맞게 단순화해서 적용한다.

---

## 8.1 Bootstrap / Incremental 수집 분리

### 문제

항상 같은 페이지 범위를 수집하면:

- 첫 실행 때 과거 데이터가 충분히 안 들어오거나
- 반대로 평상시에도 불필요하게 오래된 페이지를 계속 요청한다.

### Bootstrap

최초 실행 또는 해당 출처 데이터가 없는 상황에서는 넓게 수집한다.

예:

```text
최초 실행
→ 최근 5페이지
→ 최대 150건
```

목적은 초기 DB를 어느 정도 채우는 것이다.

### Incremental

정상 운영 중에는 최근 범위만 확인한다.

예:

```text
20분마다
→ 1페이지
→ 최대 30건
→ 기존 external_id가 충분히 오래 연속 등장하면 중단
```

더 발전하면 마지막 수집 시각 또는 최근 확인한 게시글 기준으로 중단할 수 있다.

### 추천 구현 순서

처음부터 복잡한 cursor 시스템을 만들지 않는다.

1차:

```text
bootstrapMaxPages
bootstrapMaxItems
maxPages
maxItems
```

정도로 시작한다.

2차 필요 시:

```text
lastSeenExternalId
lastCollectedAt
```

기반 조기 종료를 검토한다.

---

## 8.2 출처별 runtime config

사이트마다 다음 특성이 다르다.

- 게시글 생성량
- 응답 속도
- 한 페이지 항목 수
- 허용 가능한 요청 빈도
- timeout 필요치

따라서 장기적으로 동일한 수집 설정을 강제하지 않는다.

예시:

```yaml
pickdeal:
  collectors:
    quasarzone:
      enabled: true
      interval: 10m
      timeout: 5s
      max-pages: 1
      max-items: 40
      bootstrap-max-pages: 5
      bootstrap-max-items: 200

    ruliweb:
      enabled: true
      interval: 20m
      timeout: 10s
      max-pages: 1
      max-items: 30
      bootstrap-max-pages: 3
      bootstrap-max-items: 100
```

### 각 값의 의미

- `enabled`
  - 해당 수집기를 즉시 끌 수 있게 한다.
- `interval`
  - 수집 주기.
- `timeout`
  - 외부 사이트 응답 대기 상한.
- `maxPages`
  - 한 번 실행에서 탐색할 페이지 수 상한.
- `maxItems`
  - 한 번 실행에서 처리할 게시글 수 상한.
- `bootstrapMaxPages`
  - 초기 수집 시 범위.
- `bootstrapMaxItems`
  - 초기 수집 시 처리 상한.

### 현재 단계에서 주의

출처별 동적 scheduler를 과하게 구현하지 않는다.

우선 `maxPages`, `maxItems`, `timeout`, `enabled` 정도부터 적용하고, 출처마다 실제로 다른 interval이 필요해질 때 scheduler 구조를 확장해도 된다.

---

## 8.3 수집 결과 가시성

상시 운영 전에 최소한 다음은 로그로 확인할 수 있어야 한다.

```text
source
startedAt
finishedAt
requestedPages
parsedItems
newItems
updatedItems
skippedItems
failedItems
duration
```

예:

```text
[collector][quasarzone]
pages=1 parsed=40 new=5 updated=35 failed=0 duration=820ms
```

처음부터 Prometheus/Grafana를 붙이지 않는다.

로그만으로 운영 상태를 확인하기 어려워지는 시점에 metrics를 검토한다.

---

# 9. Phase 3 — 판매처 / 상품 식별 데이터 확보

교차 출처 dedup을 제대로 하려면 제목 해시만으로는 부족하다.

현재 가장 먼저 확보해야 하는 것은 **실제 상품을 가리키는 데이터**다.

## 후보 필드

```text
shop
productUrl
canonicalProductUrl
shopProductId
normalizedProductName
modelName
optionText
```

모두 처음부터 필수로 만들 필요는 없다.

최우선은 다음 세 가지다.

```text
shop
productUrl
shopProductId (추출 가능할 때)
```

## 용어 구분

```text
Source
= 딜 게시글이 올라온 커뮤니티
  예: Quasarzone, Ruliweb

Shop
= 실제 상품을 판매하는 쇼핑몰
  예: Coupang, 11st, AliExpress
```

둘을 혼동하지 않는다.

## 상품 URL 정규화

같은 상품도 tracking parameter 때문에 URL이 달라질 수 있다.

예:

```text
https://shop.example/product/123?utm_source=a
https://shop.example/product/123?affiliate=bbb
```

가능하면 다음으로 정규화한다.

```text
shop = example
shopProductId = 123
canonicalKey = example:123
```

### 우선순위

동일 상품 판정에서 강한 증거 순서:

```text
1. same shop + same shopProductId
2. same canonical product URL
3. same model + same option
4. normalized title + similar price
```

---

# 10. Phase 4 — 교차 출처 dedup + DealGroup

이 단계가 PickDeal의 핵심 구조 개선이다.

## 10.0 착수 전제조건

> 2026-08-26 리뷰에서 추가. 상품 URL이 사실상 0%인 상태로 dedup이 머지돼 그룹이 한 건도 생기지 않았다.

dedup은 **상품 식별 데이터가 실제로 쌓인 뒤에** 의미가 생긴다. 착수 전 다음을 수치로 확인한다.

```text
1. Phase 3의 productUrl 확보율을 실측한다.
2. 확보율이 낮으면 dedup 규칙을 손보기 전에 확보율부터 올린다.
   (파서 문제인가? 요청 상한 때문인가? 출처가 정보를 안 주는가?)
3. 그룹이 0건이면 "판정이 엄격해서"인지 "후보 자체가 없어서"인지 먼저 구분한다.
```

판정 규칙을 느슨하게 푸는 것은 **마지막 수단**이다. 대개 문제는 근거 데이터가 없는 것이지 규칙이 엄격한 것이 아니다.

## 기본 원칙

**DB의 원본 게시글을 삭제하지 않는다.**

예:

```text
Quasarzone 게시글
Ruliweb 게시글
```

둘 다 `Deal` 또는 Source Post 성격의 데이터로 남긴다.

다만 목록에서는 같은 상품/딜이면 대표 항목 하나만 보여준다.

```text
                 DealGroup
               Galaxy Buds ...
                  /     \
                 /       \
        Quasarzone       Ruliweb
```

화면:

```text
Galaxy Buds ...
129,000원

퀘이사존 · 루리웹
2개 출처
```

---

## 10.1 최소 데이터 모델 후보

정확한 최종 스키마는 구현 착수 시 현재 코드를 다시 보고 결정한다.

최소 후보:

```text
deal_group
- id
- representative_deal_id
- canonical_title
- product_key (nullable)
- created_at

deal
- ...
- group_id (nullable)
```

또는 별도 mapping table을 사용할 수 있다.

현재 규모에서는 지나치게 일반화된 Product / Offer / SourcePost 모델로 한 번에 재설계하지 않는다.

---

## 10.2 중복 판정 방식

### 후보 탐색(retrieval)과 판정(scoring)을 반드시 분리한다

> 2026-08-26 리뷰에서 추가. 이 구분이 문서에 없어서 구현이 **제목 해시를 검색 키로** 써버렸고, 더 강한 근거인 상품 URL 경로조차 *제목이 같아야만* 도달할 수 있었다. 신호 우선순위와 정반대 결과다.

```text
후보 탐색(retrieval)
- 강한 근거를 각각 독립적으로 훑는다 (OR)
- productUrl 일치로 한 번, shopProductId 일치로 한 번, titleNormHash 일치로 한 번
- 하나라도 걸리면 후보

판정(scoring)
- 모아온 후보에 대해 신호 강도로 합칠지 결정
```

약한 신호(제목)를 탐색의 관문으로 쓰면 강한 신호가 무력화된다. 제목은 출처마다 작성자가 달라 같게 정규화되는 일이 드물다는 점을 기억한다.

### 신호 강도

단일 boolean rule보다 점수/근거 기반이 운영하기 쉽다.

예:

```text
same shopProductId
→ 매우 강한 일치

same canonical URL
→ 매우 강한 일치

same model + option
→ 강한 일치

title similarity 높음 + price 동일/유사
→ 후보
```

초기 예시:

```text
95 이상
→ 자동 그룹

80~94
→ 중복 후보

80 미만
→ 별도 딜
```

점수 숫자는 실제 데이터를 보고 조정한다.  
이 값 자체를 지금 고정된 도메인 규칙으로 간주하지 않는다.

---

## 10.3 제목 정규화

현재 `title_norm_hash`는 후보 탐색용으로 활용한다.

정규화 예:

```text
"[알리/국내정품] AMD 라이젠 9850X3D 멀티팩 특가"
→
"amd 라이젠 9850x3d 멀티팩"
```

제거 후보:

- 출처별 말머리
- `[판매처]`
- 특가/역대가 등의 일반 홍보 표현
- 불필요한 특수문자
- 반복 공백

유지해야 하는 것:

- 모델 번호
- 용량
- 크기
- 세대
- 제품 버전
- 묶음 수량

예:

```text
990 PRO 1TB
990 PRO 2TB
```

는 같은 상품으로 합치면 안 된다.

---

## 10.4 대표 딜 선정

동일 그룹에 여러 출처가 있을 때 대표 딜을 고르는 정책이 필요하다.

초기 추천:

1. 실제 판매처 URL / 상품 ID가 명확한 딜
2. 가격 정보가 존재하는 딜
3. 썸네일이 존재하는 딜
4. 최신 게시글

최저가만으로 대표를 정하지 않는다.  
동일 상품이어도 쿠폰/카드 조건 때문에 단순 가격 비교가 왜곡될 수 있다.

---

# 11. Phase 5 — 개인화 UX 강화

PickDeal의 핵심 차별점은 “모두에게 같은 핫딜 목록”이 아니라 개인 큐레이션이다.

현재 기능을 더 눈에 띄게 만든다.

## 후보 개선

### 관심 키워드 매칭 이유 표시

```text
[SSD 관심 키워드로 표시됨]
```

### 목록에서 빠르게 제외

예:

```text
"키보드" 제외 키워드 추가
"RTX 5070" 관심 키워드 추가
```

### 읽음 처리

```text
읽은 딜
→ 흐리게 표시
```

### 관심 없음

```text
관심 없음
→ 이후 목록에서 숨김
```

### 출처 필터 위치 개선

현재 사이드바 중심이면 모바일 접근성이 떨어질 수 있다.

카테고리/정렬 영역 근처에 출처 필터를 노출하는 방향을 검토한다.

---

# 12. Phase 6 — 가격 이력 (제외)

2026-09-01 현재 범위에서는 구현하지 않는다. 핫딜은 같은 게시글의 가격이 장기간 변하기보다 새 게시글로 다시 올라오는 경우가 많고, 아직 상시 운영 전이라 이력 활용도도 낮다. 별도 테이블·API·UI를 먼저 만드는 비용에 비해 사용자 가치가 작다고 판단했다.

향후 상시 운영 데이터에서 동일 Deal의 가격 변경이 자주 관측되고 실제 비교 요구가 생길 때만 다시 검토한다.

---

# 13. Phase 7 — 상시 운영 / 배포

현재 로컬 테스트만으로는 실제 수집 특성을 알기 어렵다.

일정 수준 이후에는 OCI/VPS에서 24시간 운영하여 실측 데이터를 확보한다.

## 기본 방향

```text
Vercel
- Next.js frontend

OCI / VPS
- Spring Boot backend
- PostgreSQL
```

필요 시 Docker Compose 사용.

## 운영 배포 전 필요한 것

### Flyway

현재 `ddl-auto:update` 의존 상태라면 운영 전에 Flyway로 기준 스키마를 고정한다.

목표:

```text
V1__init.sql
이후 변경
V2__...
V3__...
```

### profile 분리

예:

```text
application.yml
application-local.yml
application-prod.yml
```

비밀값은 환경변수/Secret으로 관리한다.

### 최소 health / collector 상태

- backend health
- DB 연결
- 마지막 수집 성공 시각
- 출처별 최근 수집 성공/실패 로그

처음부터 복잡한 observability stack은 필요 없다.

---

# 14. Phase 8 — 알림과 Worker

알림이나 AI 요약 같은 외부 호출 작업이 들어가기 시작하면 동기 처리보다 작업 상태 관리가 중요해진다.

이때 참고할 패턴:

```text
PENDING
  ↓
CLAIMED / PROCESSING
  ↓
SUCCESS

실패
  ↓
FAILED
  ↓
RETRY
```

---

## 14.1 Claim 의미

worker가 작업을 가져갈 때 먼저 자신이 처리 중이라고 표시한다.

```text
Job #100
PENDING

Worker A claim 성공
→ PROCESSING

Worker B
→ 이미 PROCESSING
→ skip
```

중복 실행을 막는다.

## 14.2 성공

```text
PROCESSING
→ SUCCESS
```

## 14.3 실패와 재시도

```text
PROCESSING
→ FAILED

retryCount += 1
nextRetryAt = ...
```

재시도 가능한 오류일 때만 다시 실행한다.

---

## 14.4 PickDeal 적용 후보

### 관심 키워드 알림

```text
신규 DealGroup
→ 관심 키워드 매칭
→ NotificationJob
→ claim
→ Push / Telegram / Email
→ SENT
```

### AI 댓글 요약

```text
댓글 변화 감지
→ SummaryJob
→ claim
→ LLM
→ 결과 저장
```

### 구매 판단 보조

```text
가격/댓글 데이터 충분
→ AdviceJob
→ LLM 또는 Rule Engine
→ 결과 저장
```

현재 수집 upsert 자체에 이 구조를 억지로 넣지 않는다.

---

# 15. Redis 도입 기준

Redis는 현재 필수가 아니다.

다음 문제가 실제로 발생하면 도입한다.

## 최근 수집 ID 캐시

```text
외부 게시글 ID 확인
→ Redis에 최근 본 ID 있음
→ DB 조회 없이 빠르게 skip
```

DB unique constraint는 최종 안전장치로 계속 유지한다.

## rate limit

출처/API별 요청 제한이 필요해질 때.

## 알림 / worker queue

비동기 작업량이 늘어 DB polling만으로 불편해질 때.

## 조회 cache

실제 API 트래픽과 DB 부하가 확인됐을 때.

### 중요한 원칙

Redis를 넣었다고 DB 정합성 제약을 제거하지 않는다.

```text
Redis
= 성능 / 일시 상태

PostgreSQL
= 최종 데이터 정합성
```

---

# 16. AI 기능 도입 시점

AI는 데이터가 충분히 쌓인 뒤 붙인다.

먼저 필요한 것:

- 여러 출처
- 교차 출처 그룹
- 댓글 수
- 가능하면 댓글 원문 또는 댓글 변화 데이터

그 후 다음 순서가 적절하다.

## 16.1 댓글 반응 요약

입력:

- 댓글 텍스트
- 추천/비추천
- 출처
- 댓글 수

출력 예:

```text
긍정
- 가격이 최근 기준으로 괜찮다는 의견 다수

주의
- 특정 카드 조건 필요
- 배송 지연 언급
```

## 16.2 구매 판단 보조

AI가 독단적으로 "사라/사지 마라"만 출력하지 않는다.

근거를 함께 제공한다.

예:

```text
판단 보조: 가격 메리트 높음

근거
- 최근 30일 최저 수준
- 3개 출처에서 동시에 언급
- 댓글 반응 대체로 긍정
- 단, 카드 할인 조건 필요
```

---

# 17. `idoyo7/hotdeal`에서 참고할 것 / 참고하지 않을 것

해당 레포는 PickDeal 전체 설계의 모델이 아니라 **운영 안정성 참고 자료**다.

## 참고 가치가 높은 부분

1. bootstrap / incremental 수집 범위 구분
2. `maxPages`, `maxItems`, timeout 같은 runtime config
3. Redis TTL 기반 최근 게시글 중복 처리
4. claim → success/fail → retry 사고방식
5. DRY_RUN
6. 수집 결과 로그
7. Docker 기반 실행 확인

## 현재 PickDeal에 필요 없는 부분

- Kubernetes Lease leader election
- 다중 Pod HA
- FMKorea 차단 우회 로직
- User-Agent 순환
- 미러 URL 자동 우회
- Playwright를 모든 출처의 기본 수집 방식으로 사용

PickDeal에서는 현재 HTTP 기반 fetch가 가능한 출처는 그대로 단순하게 유지한다.

---

# 18. 권장 작업 순서

## 18.0 작업 ↔ Phase 매핑과 진행 상태

| 작업 | Phase | 상태 |
| --- | --- | --- |
| 1. 문서 상태 동기화 | Phase 0 | 완료 |
| 2. commentCount end-to-end | Phase 1 | 완료 |
| 3. 카테고리 최소 매핑 | Phase 1 | 완료 |
| 4. Collector runtime limit | Phase 2 | 완료 |
| 5. Bootstrap / Incremental | Phase 2 | 완료 |
| 6. Shop / Product URL 조사 | Phase 3 | 완료 (`docs/notes/2026-08-23-shop-product-url-investigation.md`) |
| 7. Shop 최소 모델 도입 | Phase 3 | 완료 — 단, **상품 URL 확보율이 낮아 후속 필요** |
| 8. 교차 출처 dedup 1차 | Phase 4 | 완료 — 2026-08-26 탐색/판정 분리 수정. **아직 그룹 0건** |
| 9. DealGroup UI | Phase 4 | 완료 (표시할 그룹이 생기면 검증 필요) |
| 10. 가격 이력 | Phase 6 | 제외 (2026-09-01, 비용 대비 활용도 낮음) |
| 11. OCI 상시 배포 | Phase 7 | 미착수 |
| 12. 알림 | Phase 8 | 미착수 |
| 13. AI 댓글 요약 | Phase 9 | 미착수 |

상품 URL이 없는 과거 Deal의 상세 백필은 출처 요청 비용 대비 가치가 낮아 구현하지 않는다. 증분 수집의 `max-detail-requests`는 실제 운영에서 주기당 신규 Deal이 상한을 자주 넘을 때만 조정한다. **다음 작업은 11번 OCI 상시 배포**다.

---

## 작업 1 — 문서 상태 동기화

목표:

- Quasarzone + Ruliweb 현재 상태 반영
- 오래된 status 문구 정리

코드 변경은 최소화하거나 하지 않는다.

---

## 작업 2 — commentCount end-to-end

범위:

```text
Parser
→ CollectedDeal
→ Deal
→ Response DTO
→ api-types
→ DealCard
```

테스트 포함.

---

## 작업 3 — 카테고리 최소 매핑

범위:

- 두 출처의 명백한 동일 카테고리만 mapping
- unknown은 원문 유지
- parser가 아니라 normalize 단계에서 처리하는 방향 우선

---

## 작업 4 — Collector runtime limit

우선 적용:

- enabled
- timeout
- maxPages
- maxItems
- bootstrapMaxPages
- bootstrapMaxItems

출처별 interval은 실제 필요가 확인되면 다음 단계.

---

## 작업 5 — Bootstrap / Incremental

초기에는 단순 구현.

```text
해당 Source의 기존 Deal이 없음
→ bootstrap

있음
→ incremental
```

이후 필요 시 lastSeen 기반 조기 종료.

---

## 작업 6 — Shop / Product URL 조사

아직 스키마부터 만들지 않는다.

먼저 실제 Quasarzone / Ruliweb fixture를 기준으로 조사한다.

확인할 것:

- 실제 판매처 URL을 목록에서 얻을 수 있는가?
- 상세 페이지 요청이 필요한가?
- redirect/affiliate URL인가?
- shopProductId를 추출할 수 있는가?
- 데이터 확보 비용이 출처마다 얼마나 다른가?

조사 결과를 짧은 note로 남긴 뒤 스키마를 결정한다.

---

## 작업 7 — Shop 최소 모델 도입

작업 6 결과에 따라 최소 필드만 추가한다.

후보:

```text
shop
productUrl
canonicalProductUrl
shopProductId
```

모든 필드를 반드시 넣지 않는다.

---

## 작업 8 — 교차 출처 dedup 1차

우선 룰 기반.

후보 신호:

```text
shopProductId
canonical URL
model
option
normalizedTitle
price
```

AI/embedding 사용 금지.

---

## 작업 9 — DealGroup UI

목록:

```text
대표 딜 한 개
출처 N곳
```

상세:

```text
Quasarzone
Ruliweb
각 원문 링크
각 게시 시각
```

원본 Deal row는 유지한다.

---

## 작업 10 — 가격 이력 (제외)

현재는 구현하지 않는다. 같은 게시글의 가격 변화가 실제로 자주 관측되고 비교 요구가 확인될 때만 재검토한다.

---

## 작업 11 — OCI 상시 배포

- Dockerfile
- Compose
- PostgreSQL
- Flyway
- prod profile
- collector 로그 확인

---

## 작업 12 — 알림

실제 상시 수집이 안정화된 뒤 진행.

먼저 한 채널만 구현한다.

예:

```text
Telegram 또는 Web Push
```

처음부터 이메일 + Telegram + Discord + Web Push를 모두 만들지 않는다.

---

## 작업 13 — AI 댓글 요약

실제 댓글 데이터 확보 방식과 비용을 먼저 측정한 후 도입한다.

---

# 19. 구현하지 말아야 할 것

AI agent가 다음을 임의로 추가하지 않는다.

```text
Kafka
RabbitMQ
Elasticsearch
Kubernetes
Microservices
CQRS
Event Sourcing
Vector DB
Embedding dedup
LLM product matching
범용 crawler DSL
관리자용 low-code rule engine
```

이런 기술은 현재 PickDeal의 문제를 해결하는 데 필요하지 않다.

필요성이 실제로 발생하면 별도 ADR 또는 검토 문서를 먼저 작성한다.

---

# 20. 테스트 원칙

## Collector parser

실제 응답 HTML fixture 기반 테스트를 유지한다.

```text
HTML fixture
→ Parser
→ 예상 DTO
```

외부 사이트를 테스트 중 실시간 호출하지 않는다.

## Normalize

카테고리, 제목, 가격, 판매처 정규화는 별도 입력/출력 테스트가 가능하게 유지한다.

## Dedup

교차 출처 dedup 도입 시 테스트 케이스를 먼저 쌓는다.

반드시 포함:

```text
동일 모델 + 동일 용량 + 동일 가격
→ group

동일 모델 + 다른 용량
→ separate

제목 유사하지만 다른 제품
→ separate

동일 URL + tracking parameter만 다름
→ group
```

자동 dedup에서는 **false positive가 false negative보다 더 나쁘다.**

즉 애매하면 합치지 않는 방향을 우선한다.

---

# 21. 운영 데이터로 확인해야 할 것

> 2026-08-26 갱신: 아래 중 **확보율 계열은 상시 운영을 기다릴 필요가 없다.** 로컬에서 앱을 한 번 띄우면 측정되며, 5.1의 공통 완료 조건에 따라 각 작업에서 바로 확인한다. 배포 이후에나 의미가 생기는 것은 "하루 신규 딜 수", "timeout 빈도", "알림 발생 빈도"처럼 시간이 쌓여야 하는 값들이다.

| 지표 | 언제 측정 |
| --- | --- |
| 가격·썸네일·판매처 URL·댓글 수 확보율 | **로컬 1회 수집으로 즉시** (작업 완료 조건) |
| parser 실패율 | 로컬에서도 부분 확인 가능 |
| 하루 신규 딜 수, 평균 수집 시간, timeout 빈도 | 상시 운영 이후 |
| dedup 오탐/미탐 | 상품 URL 확보율이 오른 뒤 |

상시 운영 이후 다음 값을 실제로 관찰한다.

## 출처별

- 하루 신규 딜 수
- 평균 수집 시간
- timeout 빈도
- parser 실패율
- 가격 추출 성공률
- 썸네일 확보율
- 실제 판매처 URL 확보율

## Dedup

- 자동 그룹 성공 수
- 오탐
- 같은 딜인데 그룹되지 않은 건수
- 가장 유효한 판정 신호

## 사용자 기능

- 관심 키워드로 실제 남는 딜 수
- 제외 키워드 효과
- 읽지 않는 출처
- 알림 발생 빈도

이 데이터를 기준으로 이후 설계를 변경한다.

---

# 22. AI Agent 작업 규칙

Codex / Claude Code는 이 문서를 읽은 후 다음 방식으로 작업한다.

## 작업 시작 전

1. `AGENTS.md` 읽기
2. `CONTEXT.md` 읽기
3. 관련 `docs/01~06` 읽기
4. 실제 구현 파일 확인
5. 현재 테스트 확인
6. 문서와 코드가 충돌하면 코드 상태를 먼저 파악

## 구현 시

- 한 작업 단위만 처리
- **5.1의 공통 완료 조건을 지킨다 — fixture 테스트 통과로 끝내지 않고 실제 1회 수집해 확보율을 확인한다**
- 불필요한 새 추상화 금지
- 기존 package / naming / DTO 계약 유지
- 실제 데이터가 없는 미래 구조를 임의로 완성하지 않음
- 스키마 변경 시 `docs/04` 갱신
- API 변경 시 `docs/03` 갱신
- collector 정책 변경 시 `docs/05` 갱신
- 실제 코드와 상태 문서가 달라지면 status 문구도 같이 갱신

## 작업 종료 시 보고

다음 형식으로 간단히 정리한다.

```text
변경
- ...

테스트
- ...

설계 판단
- ...

의도적으로 하지 않은 것
- ...

다음 후보
- ...
```

---

# 23. Codex / Claude Code에 바로 전달할 첫 요청 예시

아래 요청부터 진행하는 것을 권장한다.

```text
PickDeal 저장소의 AGENTS.md, CONTEXT.md, docs/01~06과
현재 collector/quasarzone, collector/ruliweb 구현을 먼저 확인해줘.

추가로 `docs/roadmap.md`의 방향을 기준으로 작업하되,
한 번에 여러 Phase를 구현하지 말아줘.

첫 작업은 다음 두 가지다.

1. 현재 코드 상태와 문서의 status 문구를 비교해서
   Quasarzone 한 출처라고 남아 있는 오래된 표현을
   Quasarzone + Ruliweb 현재 상태에 맞게 최소 수정한다.

2. 이후 commentCount end-to-end 작업을 위한
   현재 코드 경로와 변경 파일 후보를 분석한다.
   아직 구현하지 말고, 최소 변경 계획과 테스트 포인트를 제시한다.

원칙:
- 현재 구조를 유지한다.
- 새 추상화는 실제 중복이 확인된 경우에만 만든다.
- Redis, worker, AI, 인증 등 다음 단계 기능은 건드리지 않는다.
- 문서와 코드가 충돌하면 실제 코드를 먼저 확인한다.
```

문서 정리가 끝난 다음에는 commentCount를 별도 작업으로 요청한다.

---

# 24. 최종 우선순위 요약

```text
[완료: 1~9]

1. docs 현재화                  ✔
2. 댓글 수                      ✔
3. 카테고리 최소 매핑           ✔
4. collector runtime limit      ✔
5. bootstrap / incremental      ✔
6. shop / product URL 실측 조사 ✔
7. shop 최소 데이터 모델        ✔ (상품 URL 확보율 낮음 — 후속 필요)
8. 교차 출처 dedup              ✔ (그룹 0건 — 커버리지 대기)
9. DealGroup UI                 ✔ (검증 대기)

[제외 결정]

7-1. 과거 Deal 상품 URL 백필
     - 오래된 핫딜의 활용도보다 출처 상세 요청 비용이 커서 구현하지 않는다
10. 가격 이력
     - 같은 게시글의 가격 변화가 드물고 상시 운영 전이라 현재는 구현하지 않는다

[지금 할 것 — 운영 단계]

11. OCI 상시 배포
12. Flyway / prod profile
13. collector 운영 로그
14. 필요할 때 Redis

[개인화 / 자동화]

15. 읽음 / 관심 없음
16. 관심 키워드 알림
17. claim / retry worker 처리

[데이터가 쌓인 뒤]

18. 댓글 수집
19. AI 댓글 요약
20. AI 구매 판단 보조
```

---

# 25. 핵심 판단

PickDeal의 다음 성장은 **출처 개수를 빠르게 늘리는 것**보다 다음 문제를 해결하는 데서 나온다.

```text
서로 다른 사이트에서 들어온 데이터를
얼마나 안정적으로 수집하고,

같은 상품인지 얼마나 안전하게 판별하고,

사용자에게 중복 없이 의미 있게 보여주느냐.
```

따라서 Quasarzone + Ruliweb 두 출처만으로도 당분간 핵심 기능 개발은 충분하다.

세 번째 출처를 추가하기 전에 최소한 다음은 먼저 진행하는 것을 권장한다.

```text
댓글 수
카테고리 통합
상품 식별 데이터 조사
교차 출처 dedup 기반
```

PickDeal이 단순한 "핫딜 사이트 모음"에서 벗어나는 지점은 **DealGroup + 개인화 + 가격/반응 데이터**다.

이 세 축을 우선해서 발전시킨다.
