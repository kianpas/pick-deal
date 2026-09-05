# 05. 수집기 설계 (Collector)

> 복수 출처 수집과 보수적인 교차 출처 DealGroup 연결·목록·상세 노출이 구현됐다. 현재 수집 출처 목록과 자동 그룹 규칙은 이 문서에서 관리한다.
> 최초 작성: 2026-05-20 · 현재 상태 갱신: 2026-09-05

---

## 1. 현재 범위

| 항목 | 상태 |
| --- | --- |
| 복수 출처 목록 수집·파싱·저장 | ✅ |
| 목록 HTML의 댓글 수 수집·표시 | ✅ |
| 카테고리 정확 일치 별칭 최소 정규화 | ✅ |
| 신규 Deal의 판매몰 이름·상품 URL 제한적 상세 보강 | ✅ |
| 출처별 요청 제한과 Bootstrap/Incremental 구분 | ✅ |
| 출처 내 중복 방지(`source_id + external_id`) | ✅ |
| 보수적인 교차 출처 DealGroup 연결·UI | ✅ |
| Redis, 별도 worker, 알림, AI 댓글 요약 | 미구현·보류 |

조회·설정 MVP는 시드 데이터로 시작했고 현재는 등록된 출처의 수집 결과를 함께 사용한다. Redis나 별도 worker는 수집 부하가 조회 API에 실제로 영향을 줄 때만 검토한다.

---

## 2. 현재 수집 출처

| 코드 | 표시명 | 구현 패키지 | 상태 |
| --- | --- | --- | --- |
| `quasarzone` | 퀘이사존 | `collector/quasarzone/` | 활성 |
| `ruliweb` | 루리웹 | `collector/ruliweb/` | 활성 |

기존 공통 계약 안에서 출처만 추가할 때는 **이 표, 해당 `collector/{source}/` 코드, 실제 응답 HTML 테스트 fixture**만 갱신한다. API·DB·공통 수집 구조·운영 정책이 함께 바뀌는 경우에만 관련 설계문서를 추가로 갱신한다.

새 출처를 붙이기 전에 `robots.txt`를 확인한다. 접근이 허용된 목록만 수집하며, 모바일·미러 URL을 차단 우회 목적으로 사용하지 않는다.

---

## 3. 수집 구조

현재는 단일 Spring Boot 앱 안에서 `CollectScheduler`가 등록된 `SourceCollector` 구현체를 순회한다. 전체 수집 완료 시점부터 20분 뒤 다시 실행한다.

```text
CollectScheduler
  └─ SourceCollector[]
       └─ collector/{source}/
            Client(fetch) → Parser(parse) → CollectService(normalize)
                                      │
                                      └─ collector/support/(persist·공통 지원)
```

- `SourceCollector` — 출처 하나의 수집 계약. 출처가 늘어도 스케줄러는 바뀌지 않는다.
- 출처별 `Client` — 목록·상세 HTTP 요청.
- 출처별 `Parser` — `String html → 결과`인 순수 파싱 로직. 실제 HTML fixture로 검증한다.
- 출처별 `CollectService` — 카테고리·게시 시각 등 출처 결과를 `CollectedDeal`로 정규화한다.
- `DealUpsertSupport` — 출처 등록과 `(source, external_id)` 기반 신규 저장·기존 갱신.
- `NewDealDetailSupport` — 신규 Deal만 상세 보강하고 요청 상한과 실패 격리를 적용한다.
- `DealGroupingService` — 다른 출처의 강한 일치 Deal을 원본 보존 상태로 연결한다.

재수집 시 기존 Deal은 가격·카테고리·댓글 수·상태를 최신 관측값으로 갱신한다. `collectedAt`은 현재 최초 저장 시각 성격으로 유지되므로, 출처별 최근 수집 성공 시각으로 해석하지 않는다.

---

## 4. 필드 처리 정책

### 댓글 수

`commentCount`는 목록 HTML에 이미 있는 값만 가져와 상세 요청을 늘리지 않는다. 값이 없거나 확인할 수 없으면 `null`로 둔다. 댓글 수는 반응량일 뿐 긍정 평가가 아니며 추천·정렬에는 사용하지 않는다. 그룹 목록에서는 대표 게시글 값을 보여주고 합산하지 않으며, 상세에서는 출처별 값을 표시한다.

### 카테고리

Parser는 원문 카테고리를 그대로 추출하고 `CategoryNormalizer`가 normalize 단계에서 정확히 등록된 별칭만 대표 문자열로 바꾼다. 미등록 값은 원문을 유지한다. 단어 포함·유사도 매칭이나 완성형 분류 체계는 도입하지 않는다.

### 원문·상품 링크와 판매몰

`originalUrl`은 커뮤니티 원문 링크로 유지한다. `productUrl`은 출처별 상세 Parser가 선택한 외부 HTTP(S) 상품·행사 링크이며 두 값을 합치거나 대체하지 않는다.

상품 링크는 목록의 임의 링크에서 추측하지 않는다. 기본은 광고·일반 본문 링크와 구분되는 전용 영역이다. 퀘이사존은 전용 `링크` 행이 사라진 현재 구조에 한해 본문 원본(`textarea#org_contents`) 안의 첫 외부 링크를 폴백으로 사용한다. 페이지 공통 배너·후원사 링크는 제외한다.

`shopName`은 출처가 표시한 문자열을 그대로 저장하며 표준화하지 않는다. 별도 Shop 엔티티, canonical URL, 판매몰 상품 ID는 도입하지 않는다.

상세 보강은 DB에 없는 신규 `externalId`에만 적용한다. 실패해도 목록 정보와 `productUrl = null`로 저장하며 전체 수집을 실패시키지 않는다. 상품 URL을 확인하거나 redirect를 따라가는 추가 요청은 하지 않는다. 상한 초과나 실패로 상품 URL을 얻지 못한 기존 Deal은 자동 backfill하지 않는다.

---

## 5. 교차 출처 그룹 규칙

원본 Deal 행은 삭제하거나 합치지 않고 `deal_group`과 nullable `deal.group_id`로만 연결한다. 신규 저장과 재수집 upsert 뒤 `DealGroupingService`가 다른 출처 후보를 확인한다.

제목 정규화는 Unicode 폭·대소문자·공백·기호와 **출처가 확인한 판매몰 말머리만** 제거한다. 모델명·용량·세대·수량과 판매몰이 아닌 대괄호 정보는 보존한다.

자동 그룹은 다음 두 조건을 모두 만족할 때만 연결한다.

1. 두 `productUrl`이 정확히 같거나 제목 정규화 해시가 정확히 같다.
2. 정규화한 `shopName`과 `price`가 모두 정확히 같다.

본문 폴백이 공통 기획전 링크를 가리킬 수 있으므로 URL 단독 일치는 판정 근거로 사용하지 않는다. 제목이 달라도 URL·판매처·가격이 모두 같으면 그룹화할 수 있지만, 같은 URL이어도 판매처나 가격이 다르면 그룹화하지 않는다.

한 그룹에는 출처별 Deal 하나만 연결한다. 같은 출처 후보가 여러 건이거나 서로 다른 기존 그룹이 동시에 후보가 되면 자동 그룹화를 건너뛰며 기존 그룹을 자동 병합하지 않는다. false positive가 false negative보다 위험하므로 애매하면 별도 카드로 유지한다.

유사도·점수 임계치·tracking parameter 제거·canonical URL·상품 ID 추출·AI/embedding은 운영 데이터로 필요성이 확인될 때만 검토한다.

대표 Deal은 상품 URL, 가격, 썸네일 보유 순으로 정보가 풍부한 항목을 우선하고 동률이면 최신 게시글을 선택한다. 목록은 출처 표시/필터를 먼저 적용한 뒤 그룹 단위로 접어 페이지 처리한다. 설정된 대표가 제외되면 노출 가능한 구성원 중 같은 기준으로 임시 대표를 고른다. 그룹 상태는 구성원 하나라도 활성이라면 `ACTIVE`다. 활성 상태와 대표 게시글 상태가 어긋나는 실제 사례가 확인되면 활성 대표 우선 여부를 보완한다.

---

## 6. 출처별 요청 제한

`application.yml`의 `pickdeal.collector.sources.{source-code}` 아래에서 출처별 설정을 관리한다. 각 출처의 `*CollectorProperties`가 자기 설정을 소유한다.

| 설정 | 의미 | 현재 기본값 |
| --- | --- | --- |
| `scheduling.enabled` | 전체 자동 수집 스케줄 활성 여부 | `true` |
| `sources.{code}.enabled` | 해당 출처 수집기 활성 여부 | `true` |
| `timeout` | 목록·상세 HTTP 요청 한 건의 응답 대기 상한 | `10s` |
| `max-pages` | 일반 실행의 목록 페이지 수 상한 | `1` |
| `max-items` | 일반 실행의 고유 게시글 처리 상한 | `50` |
| `bootstrap-max-pages` | 최초 수집의 목록 페이지 수 상한 | `3` |
| `bootstrap-max-items` | 최초 수집의 고유 게시글 처리 상한 | `150` |
| `max-detail-requests` | 실행당 신규 Deal 상세 요청 상한 | `3` |

해당 출처에 Deal이 하나도 없으면 Bootstrap, 하나라도 있으면 Incremental 한도를 사용한다. 페이지는 1부터 순서대로 요청하고 항목 상한에 도달하면 남은 페이지를 요청하지 않는다. 실행 안에서 같은 `externalId`가 반복되면 한 번만 처리한다.

현재 기본값의 외부 요청 상한은 Incremental에서 출처당 목록 1회와 상세 3회, Bootstrap에서 목록 3회와 상세 3회다. `lastSeenExternalId`, 출처별 최근 성공 시각, 별도 cursor와 출처별 스케줄 주기는 아직 없다.

필드 확보율 로그는 **이번 수집 입력**의 값이며 DB 전체 보유율이나 상세 요청 성공률이 아니다. 상시 운영 단계에서 필요하면 상세 요청 수·성공 수와 저장 데이터 보유율을 분리해 관측한다.

---

## 7. 남은 경계

- 출처별 최근 수집 성공 시각과 실패 상태는 상시 운영 준비에서 다룬다.
- 마지막 게시글 기반 조기 종료는 페이지 요청 절감 필요가 확인될 때 검토한다.
- 목록 전체 메모리 처리, 수집 HTTP 요청을 포함한 트랜잭션은 운영 데이터에서 병목이나 장애 영향이 확인되면 개선한다.
- Redis·별도 worker는 수집 부하가 조회 API에 영향을 줄 때만 도입한다.
- 알림과 AI 댓글 요약은 실제 데이터와 사용 요구가 확인된 뒤 별도 설계한다.

---

## 8. 관련 문서

- API와 그룹 응답: `docs/03-api-design.md`
- 스키마와 제약: `docs/04-database-design.md`
- 배포 및 운영 준비: `docs/06-deployment.md`
- 구현 과정과 조사 기록: `docs/notes/`
