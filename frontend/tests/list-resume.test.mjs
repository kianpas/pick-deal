import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { test, beforeEach } from "node:test";
import ts from "typescript";

// 기존 TypeScript 의존성으로 순수 모듈만 변환한다. 테스트 전용 런타임 의존성은 추가하지 않는다.
async function loadModule(path) {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } });
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`);
}
const { saveListSnapshot, takeListSnapshot, clearListSnapshot } = await loadModule("../lib/list-resume.ts");
const { listLocation, hasSearchFilters, resetSearchLocation } = await loadModule("../lib/list-location.ts");
const KEY = "pickdeal:list-resume:v1";
let store;
beforeEach(() => {
  store = new Map();
  globalThis.sessionStorage = {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => store.set(key, value),
    removeItem: (key) => store.delete(key),
  };
});
function snapshot() {
  return { listHref: "/?sourceId=3", deals: Array.from({ length: 40 }, (_, i) => ({ id: i + 1, title: `상품 ${i + 1}` })),
    nextPage: 2, hasNext: true, selectedDealId: 35, scrollY: 3000, viewportWidth: 390, showThumbnail: false };
}
test("40개 목록·다음 페이지·위치·썸네일 설정을 같은 조건 복귀에서 한 번 복원한다", () => {
  saveListSnapshot(snapshot());
  const saved = takeListSnapshot("/?sourceId=3", "35");
  assert.equal(saved.deals.length, 40);
  assert.equal(saved.nextPage, 2);
  assert.equal(saved.scrollY, 3000);
  assert.equal(saved.showThumbnail, false);
  assert.equal(takeListSnapshot("/?sourceId=3", "35"), null);
});
test("브라우저 뒤로 가기도 같은 탭 스냅샷을 사용할 수 있다", () => {
  saveListSnapshot(snapshot());
  assert.equal(takeListSnapshot("/?sourceId=3", null).selectedDealId, 35);
});
test("다른 필터나 다른 상품의 복귀에서는 복원하지 않는다", () => {
  saveListSnapshot(snapshot());
  assert.equal(takeListSnapshot("/?sourceId=1", "35"), null);
  saveListSnapshot(snapshot());
  assert.equal(takeListSnapshot("/?sourceId=3", "5"), null);
});
test("오래된 데이터·손상된 저장값·출처 설정 변경 시 복원하지 않는다", () => {
  store.set(KEY, JSON.stringify({ ...snapshot(), savedAt: Date.now() - 16 * 60 * 1000 }));
  assert.equal(takeListSnapshot("/?sourceId=3", "35"), null);
  store.set(KEY, "broken-json");
  assert.equal(takeListSnapshot("/?sourceId=3", "35"), null);
  saveListSnapshot(snapshot());
  clearListSnapshot();
  assert.equal(takeListSnapshot("/?sourceId=3", "35"), null);
});
test("저장소 차단이나 용량 초과가 링크 이동을 막지 않는다", () => {
  globalThis.sessionStorage = {
    getItem() { throw new Error("blocked"); },
    setItem() { throw new Error("quota"); },
    removeItem() { throw new Error("blocked"); },
  };
  assert.doesNotThrow(() => saveListSnapshot(snapshot()));
  assert.equal(takeListSnapshot("/?sourceId=3", "35"), null);
});
test("목록 복귀 URL은 안전한 필터와 양의 상품 ID만 허용한다", () => {
  assert.equal(listLocation("https://evil.example/?resume=35"), "/");
  assert.equal(listLocation("/?sourceId=3&resume=35&unknown=1"), "/?sourceId=3&resume=35");
  assert.equal(listLocation("/?sourceId=3&resume=35", false), "/?sourceId=3");
  assert.equal(listLocation("/?resume=-1"), "/");
  assert.equal(listLocation("/?resume=9007199254740992"), "/");
});

test("검색어·쇼핑몰 없이 커뮤니티·카테고리·종료 숨기기만 있어도 초기화할 조건으로 판별한다", () => {
  for (const query of ["q=노트북", "shopName=쿠팡", "sourceId=2", "category=DIGITAL", "hideEnded=true"]) {
    assert.equal(hasSearchFilters(new URLSearchParams(query)), true, query);
  }
  for (const query of ["", "page=2&resume=35", "q=%20&shopName=", "hideEnded=false", "sourceId=-1", "sourceId=9007199254740992"]) {
    assert.equal(hasSearchFilters(new URLSearchParams(query)), false, query);
  }
});

test("초기화는 중복 탐색 조건과 페이지·복귀 표식을 제거하고 입력 URL을 변경하지 않는다", () => {
  const params = new URLSearchParams("q=노트북&category=DIGITAL&sourceId=1&sourceId=2&shopName=쿠팡&shopName=네이버&hideEnded=true&page=3&resume=35");
  assert.equal(resetSearchLocation(params), "/");
  assert.equal(params.get("resume"), "35");
  assert.equal(params.getAll("sourceId").length, 2);
  params.set("sort", "latest");
  assert.equal(resetSearchLocation(params), "/?sort=latest");
  assert.equal(hasSearchFilters(new URLSearchParams(resetSearchLocation(params).slice(2))), false);
});
