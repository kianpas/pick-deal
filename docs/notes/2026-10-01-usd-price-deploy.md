# USD 가격 지원 배포 (V2 migration)

> 2026-10-01 · `docs/06`에서 옮긴 일회성 배포 절차. 현재 가격 계약은 `docs/03`·`docs/04`를 따른다.

V2 Flyway migration은 `deal.price`·`original_price`를 `numeric(21,2)`로 변경하며 기존 원화 금액은 유지한다. 운영 DB 백업 후 backend를 먼저 배포하여 migration과 기동을 확인하고, 로컬 수집기 이미지를 다시 빌드·실행한다. 프론트는 USD를 `US$382.76`처럼 환산 없이 표시한다. 기존 가격 누락 행은 목록 재수집 시 갱신되며, 목록 범위를 벗어난 과거 글은 자동으로 다시 상세 조회하지 않는다.
