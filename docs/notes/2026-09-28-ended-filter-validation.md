# 종료·품절 숨기기 검증

## 범위

- 저장된 상태만 사용하며 외부 사이트 요청은 추가하지 않는다.
- 표시 출처·쇼핑몰 조건을 통과한 그룹에 ACTIVE 구성원이 하나라도 있으면 유지한다.
- 그룹 필터 후 페이지를 나누므로 전체 건수와 더 보기에도 같은 조건을 적용한다.
- 퀘이사존은 목록 종료 표시를 재수집 시 갱신한다. 개드립은 취소선 관측 시 종료를 반영한다. 뽐뿌는 현재 종료 상태를 수집하지 않는다. 오래된 글이나 미관측 상태의 실제 판매 여부를 보장하지 않는다.

## 로컬 성능 비교

2026-09-28 Windows 로컬 Java 17, H2 in-memory, MockMvc에서 수행했다. 운영 DB·외부 사이트에는 요청하지 않았다.

- 합성 게시글 2,000건, ACTIVE/EXPIRED 각각 1,000건, 그룹 없는 데이터
- 응답 페이지 크기 20, 두 조건 실행 순서를 교대
- 워밍업 조건별 10회 제외, 조건별 30회 측정
- 매 요청 전 JPA 영속성 컨텍스트를 비워 1차 캐시 재사용을 피함
- DB 조회·서비스·JSON 응답 처리 포함, HTTP 네트워크·Vercel SSR·OCI 자원 제한은 미포함

| 조건 | 중앙값 | P95 |
| --- | ---: | ---: |
| OFF | 13.46ms | 26.21ms |
| ON | 13.04ms | 20.12ms |

이 표본에서는 큰 추가 지연이 관찰되지 않았다. ON이 항상 빠르다는 결론이나 OCI 1 CPU·6GB의 처리량 보장은 아니다. 실제 데이터 분포·그룹 수·동시 요청·PostgreSQL I/O에 따라 달라진다. 전체 Deal을 읽는 기존 구조는 유지했으며 데이터 증가에 따른 비용은 별도 문제다.

재실행(backend 디렉터리, PowerShell):

```powershell
$env:PICKDEAL_FILTER_BENCHMARK='true'
.\gradlew.bat test --tests com.pickdeal.deal.api.EndedDealFilterTest
Remove-Item Env:PICKDEAL_FILTER_BENCHMARK
```

결과는 `build/test-results/test/TEST-com.pickdeal.deal.api.EndedDealFilterTest.xml`의 `FILTER_BENCHMARK`에 기록된다. 환경변수가 없으면 성능 비교만 건너뛰며 기능 테스트는 실행한다.

## 화면 검증

임시 H2 백엔드(수집기 OFF)와 로컬 Next.js에서 확인했다. 모바일 320·375·390·430px 및 PC 1280px에서 문서 가로 넘침 없이 종료 필터 라벨 높이 44px를 확인했다. 키보드 Space로 토글, URL 반영, 적용 중 기존 목록 유지, 적용 완료 후 체크박스 포커스 복원을 확인했다.

OCI 실측, 실기기, 스크린리더, 200% 확대·RTL 검증은 수행하지 않았다. 테스트 데이터는 실제 판매 여부나 운영 이미지 표시를 검증하기 위한 것이 아니다.
