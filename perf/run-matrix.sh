#!/usr/bin/env bash
# 데이터 양·동시 사용자 조합별로 합성 데이터를 다시 만들고 k6를 실행한다. 저장소 루트에서 실행한다.
# 사용: ENV_FILE=<측정용 env 파일> PROM=http://127.0.0.1:29090 perf/run-matrix.sh "2287:1,10 5000:1 15000:1,10"
#       ONLY=categories,shops LABEL_PREFIX=after- ...  처럼 일부 엔드포인트만 재고 결과 이름을 구분할 수 있다.
# 각 실행 뒤 backend 힙 최대·GC 시간을 측정용 Prometheus에서 읽어 perf/results/<label>.jvm.txt에 남긴다.
set -euo pipefail
# Windows Git Bash가 /scripts 같은 컨테이너 경로를 Windows 경로로 바꾸지 않게 한다. Linux에서는 영향이 없다.
export MSYS_NO_PATHCONV=1

ENV_FILE="${ENV_FILE:?측정용 env 파일 경로를 ENV_FILE로 지정한다}"
PROM="${PROM:-http://127.0.0.1:29090}"
MATRIX="${1:-2287:1,10 5000:1 10000:1 15000:1,10 30000:1,10}"
compose=(docker compose -p pickdeal-perf --env-file "$ENV_FILE" -f compose.yml -f perf/compose.perf.yml)

prom() { curl -s --get "$PROM/api/v1/query" --data-urlencode "query=$1" | sed -n 's/.*"value":\[[^,]*,"\([^"]*\)".*/\1/p'; }

for entry in $MATRIX; do
  n="${entry%%:*}"
  for vus in ${entry#*:}; do
    vus="${vus//,/ }"
    for v in $vus; do
      label="${LABEL_PREFIX:-}n${n}-vu${v}"
      echo "== $label: 데이터 ${n}건 생성"
      "${compose[@]}" exec -T postgres sh -c 'psql -q -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v n='"$n" < perf/seed-deals.sql | tail -3
      start=$(date +%s)
      "${compose[@]}" --profile perf run --rm --quiet-pull k6 run -q -e VUS="$v" -e LABEL="$label" \
        -e ONLY="${ONLY:-}" /scripts/deals.js
      sleep 70  # Prometheus 수집 간격(60초)보다 길게 기다려 마지막 구간까지 반영한다.
      window=$(( $(date +%s) - start ))
      {
        echo "heap_max_mib=$(prom "max_over_time(sum(jvm_memory_used_bytes{area=\"heap\"})[${window}s:15s]) / 1048576")"
        echo "gc_pause_seconds=$(prom "sum(increase(jvm_gc_pause_seconds_sum[${window}s]))")"
        echo "cpu_avg=$(prom "avg_over_time(process_cpu_usage[${window}s])")"
      } | tee "perf/results/${label}.jvm.txt"
    done
  done
done
