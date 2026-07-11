# 05. 수집기 / 확장 방향 (Collector)

> Quasarzone 한 출처의 수집 파이프라인은 구현됐다. 교차 출처 dedup·Redis·worker 분리·AI·알림은 아직 방향만 유지한다.
> 문서 뒤의 **부록 A**는 초기 탐색 메모다. 현재 코드와 다른 예시는 구현 지침이 아니며, 실제 필요가 생길 때 다시 설계한다.
> 최초 작성: 2026-05-20 · 현재 상태 갱신: 2026-07-11

---

## 1. 단계 구분 (요약)

| 항목 | 단계 | MVP |
| --- | --- | --- |
| Quasarzone 수집(crawl/fetch), 파싱·저장 | 2차 진입 | ✅ |
| 출처 내 중복 방지(`source_id + external_id`) | 2차 진입 | ✅ |
| 교차 출처 중복 제거(dedup) | 이후 | ❌ (`title_norm_hash` 자리만 대비) |
| Redis / worker 분리 / 알림 | 2차 | ❌ |
| AI 댓글 요약 / 구매 판단 보조 | 3차 | ❌ |

조회·설정 MVP는 시드/수동 데이터로 시작했고, 현재 Quasarzone 수집 결과도 함께 사용한다(`docs/01`, `docs/04`).

---

## 2. 현재 구현과 다음 경계

현재는 단일 Spring Boot 앱 안에서 `CollectScheduler`가 Quasarzone 수집을 주기 실행한다. `collector/quasarzone/` 안에서 **Client(fetch) → Parser(parse) → CollectService(normalize·dedup·persist)**로 역할을 나누며, 파서는 실제 HTML fixture로 검증한다. 같은 출처의 중복은 조회와 `source_id + external_id` 유니크 제약으로 막는다.

두 번째 출처도 우선 같은 하위 패키지 구조로 추가한다. 두 구현에서 실제로 반복되는 흐름이 확인되면 그때 공통 인터페이스를 추출한다. 교차 출처 dedup은 `title_norm_hash`를 후보 키로 검토하되 아직 판정 규칙을 확정하지 않는다. 수집 부하가 조회 API에 영향을 줄 때만 worker 분리와 Redis를 검토한다. AI 요약·구매 판단·알림은 착수 시 별도 설계한다.

---

## 3. 관련 문서

- 확장 아키텍처(개념): `docs/02-architecture.md` 2.2
- dedup용 스키마(이미 반영): `docs/04-database-design.md`

---
---

# 부록 A — 탐색 설계 메모 (미확정)

> 아래는 2차·3차 방향을 미리 그려본 **역사적 스케치**다. **확정 설계가 아니며**, 현재 구현과 충돌하면 본문과 코드를 우선한다.
> 특히 `SourceFetcher` 인터페이스는 아직 구현하지 않았다. 두 번째 출처에서 공통점이 확인되기 전까지는 출처별 Client·Parser·CollectService 구성을 유지한다.

## A.1 수집기 구조 (2차 초기 — 단일 앱 내 scheduler)

```
Spring Boot App
└─ scheduler (@Scheduled)
   └─ CollectService
      ├─ SourceFetcher (출처별 구현)   ──> 원문 수집
      ├─ Normalizer                    ──> 표준 Deal 형태로 변환
      ├─ DedupChecker                  ──> 중복 판별
      └─ DealRepository.save           ──> 저장
```

- 수집 대상이 적을 때는 `docs/02`의 단일 애플리케이션 안에서 `@Scheduled` 잡으로 시작한다.
- 출처별 차이는 `SourceFetcher` 인터페이스 구현체로 흡수한다.

## A.2 수집 파이프라인 단계

1. **Fetch**: 출처별 fetcher가 원문(목록/상세/댓글)을 가져온다(HTTP, RSS 등). 실제 크롤링 규칙·robots·rate limit 준수는 구현 시 정의.
2. **Parse**: 원문에서 필드 추출(제목, 가격, 정가, 링크, 게시시각, `external_id` 등).
3. **Normalize**: 표준 `Deal` 스키마(`docs/04` 2.2)로 변환. 통화/금액 정수화, 카테고리 매핑, 제목 정규화.
4. **Dedup**: 중복 판별(A.4).
5. **Persist**: 신규/갱신 저장. 유니크 제약(`source_id + external_id`) 위반 시 갱신(upsert) 처리.
6. **(3차) Enrich**: 댓글 수집·AI 요약, 구매 판단 보조 데이터 생성(A.6).

## A.3 출처별 fetcher 인터페이스 (개념)

```java
public interface SourceFetcher {
    SourceCode supports();                 // 어떤 출처를 담당하는가
    List<RawDeal> fetchDeals();            // 원문 수집
    List<RawComment> fetchComments(String externalId); // (3차) 댓글
}
```

- 새 출처 추가 = 새 구현체 추가. 코어 파이프라인은 변경하지 않는다(OCP).

## A.4 중복 딜 제거 (Dedup)

> 같은 딜이 여러 출처/여러 글로 올라오는 문제를 해결한다.

**두 단계 키**

1. **출처 내 중복(1차 키)**: `source_id + external_id` 유니크 제약으로 동일 출처의 동일 글 재삽입을 막는다(이미 MVP 스키마에 존재).
2. **교차 출처 중복(2차)**: `title_norm_hash`로 후보를 묶는다.
   - 제목 정규화: 소문자화, 공백/특수문자 정리, 브래킷·말머리(`[정보]` 등) 제거, 핵심 토큰 추출.
   - 가격·상품 식별자(모델명 등)가 함께 일치하면 동일 딜로 판단.

**그룹핑 모델**

- `deal.group_id` 또는 `deal_group` 테이블로 대표 딜 + 묶인 딜을 표현(`docs/04` 7장).
- 목록에서는 그룹의 대표 딜만 노출하고, 상세에서 "다른 출처 N곳" 형태로 함께 보여준다.

**Redis 활용(중복 수집 방지)**

- 최근 수집한 `external_id`/해시를 Redis에 단기 캐시(`SET`/`TTL`)해, 짧은 주기 재수집 시 DB 조회 없이 빠르게 스킵.

## A.5 Redis 도입 가능 지점

> MVP 필수 아님. 아래 필요가 생기는 시점에 2차 구성으로 추가한다(`docs/02` 2.2, `docs/06`).

| 용도 | 설명 | 도입 트리거 |
| --- | --- | --- |
| 중복 수집 방지 | 최근 수집 키 캐시로 재수집 스킵 | 수집 주기가 짧고 출처가 많아질 때 |
| 캐시 | 목록/상세/출처 목록 등 조회 캐시 | 조회 트래픽 증가, DB 부하 발생 시 |
| 작업 큐 | 수집/요약 작업을 워커로 비동기 분배 | worker 분리 시(A.7) |
| rate limit | 출처별 요청 속도 제한, API 보호 | 외부 요청/공개 API 늘어날 때 |

- 도입 시 Redis도 Docker Compose 서비스로 추가한다(`docs/06`).

## A.6 AI 댓글 요약 / 구매 판단 보조 (3차)

**댓글 반응 요약 파이프라인**

```
RawComment[] ──(전처리/필터)──> 요약 입력 ──(LLM 호출)──> 요약 결과 저장
```

- 입력: 출처의 댓글 텍스트(스팸/광고 1차 필터링 후).
- 처리: LLM에 "긍정/부정 비율, 핵심 장단점, 주의사항"을 구조화 요약으로 요청.
- 저장: `deal_comment_summary`(딜 FK, 요약 본문, 감정 비율, 사용 모델/버전, 생성 시각).
- 노출: `GET /api/v1/deals/{id}/comment-summary`(`docs/03` 5장).
- 비용/속도 제어: 요약은 비동기 워커에서 수행하고 결과를 캐시(Redis)한다. 동일 딜 재요약은 댓글 변동 임계치 초과 시에만.

**AI 구매 판단 보조**

- 입력 신호: 가격/정가/할인율, 가격 추이(이력 누적 시), 댓글 요약(감정/장단점), 사용자의 관심 키워드.
- 출력: "지금 살 만한가"에 대한 보조 점수/근거 요약(확정 추천이 아닌 보조 정보임을 명시).
- 저장/노출: `GET /api/v1/deals/{id}/buy-advice`.
- **주의**: 구매 판단은 보조 정보이며 최종 결정은 사용자 몫임을 UI/응답에 명확히 표기한다.

**AI 연동 원칙**

- LLM 호출은 백엔드(또는 worker)에서만 수행하고, 키/비용은 서버에서 관리한다.
- 모델 버전·프롬프트 버전을 결과와 함께 저장해 재현성/추적성을 확보한다.

## A.7 backend ↔ collector worker 분리

- **분리 기준**: 수집 대상/주기가 늘어 **수집 부하가 API 응답에 영향**을 주기 시작하면 분리한다.
  - **Backend API**: 조회/설정 API 전담(읽기 중심), 사용자 트래픽 처리.
  - **Collector Worker**: 수집·정규화·dedup·(3차) AI 요약 전담(쓰기/배치 중심).
- **통신 방식**: 공유 PostgreSQL(가장 단순) + 필요 시 작업 큐(Redis). worker는 무상태로 두어 수평 확장.
- **코드 구성 옵션**: 동일 코드베이스에서 실행 프로파일/엔트리포인트 분리(예: `--app=api` / `--app=worker`)로 시작 → 더 필요해지면 모듈/저장소 분리 검토. 컨테이너 구성은 `docs/06`.

## A.8 관심 키워드 알림 (2차)

- 신규 딜 저장 시, 사용자 관심 키워드와 매칭되면 알림 후보로 등록.
- 채널(웹 푸시/이메일 등)은 구현 시 결정. 발송 이력은 `notification` 테이블(`docs/04` 7장).
- 중복 알림 방지/속도 제어에 Redis 활용 가능.
