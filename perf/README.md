# 목록 API 부하 측정

목록 조회는 표시 대상 Deal 전체를 메모리로 읽어 필터·그룹·정렬·페이지 처리를 한다(`DealService.findDeals`).
데이터가 늘었을 때 응답 시간이 목표 안에 있는지 합성 데이터로 측정한다. 판단 기준과 결과는
`docs/notes/2026-10-19-collection-concurrency-and-observability.md`의 C-3을 따른다.

**운영 서버와 운영 DB에는 실행하지 않는다.** 아래는 모두 로컬 Docker의 별도 Compose 프로젝트(`pickdeal-perf`)와
측정 전용 DB(`pickdeal_perf`)에서만 동작한다. `seed-deals.sql`은 다른 DB 이름에서는 실행을 거부한다.

## 구성

| 파일 | 역할 |
| --- | --- |
| `compose.perf.yml` | 루트 `compose.yml`에 덧씌워 backend CPU를 1개로 제한(운영 1 OCPU)하고 k6 서비스를 추가 |
| `seed-deals.sql` | 기존 Deal·그룹·출처를 지우고 `n`건 합성 Deal과 약 5% 교차 출처 그룹을 만든다. 같은 `n`이면 같은 데이터 |
| `k6/deals.js` | 엔드포인트별 예열 뒤 측정(목록·검색·카테고리·커뮤니티·5페이지·카테고리 목록·쇼핑몰 목록·상세) |
| `run-matrix.sh` | 데이터 양·동시 사용자 조합을 차례로 실행하고 힙·GC·CPU를 Prometheus에서 읽는다 |
| `results/` | 원본 결과(JSON·로그). Git에서 제외한다 |

합성 데이터 분포: 출처 비율 퀘이사존 38% · 개드립 33% · 쿨엔조이 29%(운영 신규 비율), 게시 시각 최근 90일,
상태 ACTIVE 85% · EXPIRED 10% · SOLD_OUT 5%, 설명 30%, 가격 없음 10%. 키워드는 등록하지 않는다.

## 실행

저장소 루트에서 측정용 env 파일을 만든다. 운영 `.env`를 쓰지 않는다.

```dotenv
DB_NAME=pickdeal_perf
DB_PASSWORD=<임의 값>
GRAFANA_ADMIN_PASSWORD=<임의 값>
COMPOSE_PROFILES=monitoring
BACKEND_PORT=28080
MANAGEMENT_PORT=28081
PROMETHEUS_PORT=29090
GRAFANA_PORT=23000
COLLECTOR_ENABLED=false
```

```bash
docker compose -p pickdeal-perf --env-file <env 파일> -f compose.yml -f perf/compose.perf.yml up -d --build
ENV_FILE=<env 파일> PROM=http://127.0.0.1:29090 bash perf/run-matrix.sh "2287:1,10 15000:1,10"
```

- 한 실행은 엔드포인트 8개 × (예열 15초 + 측정 30초)로 약 8분이다. `WARMUP`·`MEASURE` 환경변수로 바꿀 수 있다.
- 측정 중 Grafana(`http://127.0.0.1:23000`)의 `PickDeal 운영` 대시보드로 힙·GC·DB 연결을 함께 본다.
- 정리: `docker compose -p pickdeal-perf --env-file <env 파일> -f compose.yml -f perf/compose.perf.yml --profile monitoring --profile perf down -v`
  (프로필을 함께 지정해야 Prometheus·Grafana와 그 볼륨까지 지워진다)

## 해석 주의

- 로컬(x86)과 운영(OCI ARM64 1 OCPU)은 CPU가 달라 절대값이 다를 수 있다. 운영과 같은 데이터 양(2,287건)을
  먼저 재서 운영 지표의 p95와 비교한 비율로 보정한다.
- k6는 backend와 같은 네트워크 공간에서 호출해 Caddy·인터넷 구간은 포함하지 않는다. 사용자 체감 시간은 이보다 길다.
- 운영 데이터를 복사하지 않으므로 제목·설명 길이와 검색어 적중률은 운영과 다를 수 있다.
