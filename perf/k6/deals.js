// 목록·필터·상세 API 응답 시간 측정. 엔드포인트별로 WARMUP 예열 뒤 MEASURE 동안 측정하고, 엔드포인트를 차례로 돈다.
// 실행: k6 run -e VUS=1 -e LABEL=n15000-vu1 /scripts/deals.js  (perf/README.md)
import http from "k6/http";
import { check } from "k6";
import { Trend } from "k6/metrics";

const BASE = __ENV.BASE_URL || "http://127.0.0.1:8080";
const VUS = Number(__ENV.VUS || 1);
const WARMUP = __ENV.WARMUP || "15s";
const MEASURE = __ENV.MEASURE || "30s";
const LABEL = __ENV.LABEL || "run";

// 이름 → 요청 경로. 상세는 setup에서 받은 실제 ID 중 하나를 고른다.
const ENDPOINTS = {
  list: "/api/v1/deals?size=20",
  search: "/api/v1/deals?size=20&q=SSD",
  category: "/api/v1/deals?size=20&category=PC",
  source: "/api/v1/deals?size=20&sourceId=1",
  page5: "/api/v1/deals?size=20&page=4",
  categories: "/api/v1/deals/categories",
  shops: "/api/v1/deals/shops",
  detail: null,
};

// ONLY=categories,shops처럼 지정하면 그 엔드포인트만 잰다(개선 전후 비교용). 비우면 전체.
const ONLY = (__ENV.ONLY || "").split(",").map((s) => s.trim()).filter(Boolean);
for (const name of Object.keys(ENDPOINTS)) {
  if (ONLY.length && !ONLY.includes(name)) delete ENDPOINTS[name];
}

const trends = Object.fromEntries(Object.keys(ENDPOINTS).map((n) => [n, new Trend(`latency_${n}`, true)]));

function seconds(d) {
  return Number(d.replace("s", ""));
}

// 엔드포인트마다 예열 시나리오와 측정 시나리오를 이어서 배치한다.
const scenarios = {};
let offset = 0;
for (const name of Object.keys(ENDPOINTS)) {
  for (const phase of ["warmup", "measure"]) {
    const duration = phase === "warmup" ? WARMUP : MEASURE;
    scenarios[`${name}_${phase}`] = {
      executor: "constant-vus",
      vus: VUS,
      duration,
      startTime: `${offset}s`,
      exec: "hit",
      env: { ENDPOINT: name, PHASE: phase },
      gracefulStop: "5s",
    };
    offset += seconds(duration) + 5;
  }
}

export const options = {
  scenarios,
  summaryTrendStats: ["avg", "med", "p(95)", "p(99)", "max", "count"],
  thresholds: { http_req_failed: ["rate<0.01"] },
};

export function setup() {
  const res = http.get(`${BASE}/api/v1/deals?size=100`);
  // 목록 응답은 data가 Deal 배열이고 페이지 정보는 meta에 있다(docs/03).
  const ids = (res.json("data") || []).map((d) => d.id);
  if (ids.length === 0) throw new Error("측정할 Deal이 없다. seed-deals.sql을 먼저 실행한다.");
  return { ids };
}

export function hit(data) {
  const name = __ENV.ENDPOINT;
  const path = ENDPOINTS[name] ?? `/api/v1/deals/${data.ids[Math.floor(Math.random() * data.ids.length)]}`;
  const res = http.get(`${BASE}${path}`, { tags: { endpoint: name, phase: __ENV.PHASE } });
  check(res, { "status 200": (r) => r.status === 200 });
  if (__ENV.PHASE === "measure") trends[name].add(res.timings.duration);
}

export function handleSummary(data) {
  const rows = {};
  for (const name of Object.keys(ENDPOINTS)) {
    const m = data.metrics[`latency_${name}`];
    if (m) rows[name] = m.values;
  }
  const out = { label: LABEL, vus: VUS, measure: MEASURE, failedRate: data.metrics.http_req_failed?.values.rate, endpoints: rows };
  const text = Object.entries(rows)
    .map(([n, v]) => `${n.padEnd(11)} p50=${v.med.toFixed(1)}ms p95=${v["p(95)"].toFixed(1)}ms p99=${v["p(99)"].toFixed(1)}ms n=${v.count}`)
    .join("\n");
  return { [`/results/${LABEL}.json`]: JSON.stringify(out, null, 2), stdout: `${LABEL} (VU ${VUS})\n${text}\n` };
}
