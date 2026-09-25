# CONTEXT.md

PickDeal에서 반복적으로 다시 설명하게 되는 **용어**와 **이미 있어 재사용할 자산**의 색인.
설계 상세는 여기 적지 않는다 — 정의 한 줄 + 코드/문서 포인터만. (규칙·절차는 `AGENTS.md`, 결정 이력은 `docs/adr/`)

> 추가 기준: "AI에게 같은 걸 두 번 설명했다" 싶을 때만 한 줄 늘린다. 미리 채우지 않는다.

## 용어

- **출처 (source)** — 딜을 긁어온 커뮤니티/딜 사이트(루리웹·퀘이사존 등). **판매처가 아니다.** 엔티티 `Source` ↔ `/api/v1/sources`. (→ `backend/.../source/domain/Source.java`)
- **판매처 (shop)** — 실제로 파는 쇼핑몰(쿠팡·알리 등). 출처와 다른 개념. `Deal.shopName`에 원문을 저장하고, 목록·필터에만 `ShopFilterNames`의 등록 별칭을 적용한다. 별도 Shop 엔티티는 없다. 상품 그룹 판정의 판매처 비교는 별도 규칙이다. (→ `docs/03` §2.2.1, `docs/05` §5)
- **DealGroup** — 서로 다른 출처의 동일 딜로 강하게 판정된 원본 `Deal`을 삭제하지 않고 `group_id`로 연결한 묶음. 목록은 대표 카드 하나로 접고 상세에서는 출처별 원문을 유지한다. (→ `backend/.../deal/domain/DealGroup.java`)
- **관심/제외 키워드 (INTEREST / EXCLUDE)** — 관심: 하나라도 등록되면 그 단어를 포함한 딜만 노출. 제외: 포함되면 딜을 숨김. 우선순위 규칙 → `docs/01` §3.2, 적용은 `DealService`.
- **닫힌 집합 (closed set)** — 값 추가가 곧 처리 로직 추가 + 배포인 도메인(`DealStatus`, `KeywordType`). 그래서 DB 룩업 테이블이 아니라 코드 enum(`@Enumerated(STRING)`)으로 관리한다. (배경 → `docs/adr/0001`)

## 자산 (재사용 — 새로 만들기 전에 여기부터)

- **`ApiResponse<T>` 봉투** — 모든 백엔드 응답은 `{ data, meta?, error? }` 형태. 새 엔드포인트도 이걸로 감싼다. (`backend/.../common/response/`)
- **`frontend/lib/api-types.ts`** — 프론트-백 **계약 타입의 SSOT**, 백엔드 DTO와 1:1. 데모용 `frontend/lib/types.ts`(Deal 등)와 혼동 금지.
