import { readFile } from "node:fs/promises";
import { createRequire } from "node:module";
import assert from "node:assert/strict";
import { test } from "node:test";
import { createElement } from "react";
import { renderToStaticMarkup } from "react-dom/server";
import ts from "typescript";

const require = createRequire(import.meta.url);
async function loadTs(path, overrides = {}) {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX, esModuleInterop: true },
  });
  const compiled = { exports: {} };
  new Function("require", "module", "exports", outputText)(
    (id) => overrides[id] ?? require(id), compiled, compiled.exports,
  );
  return compiled.exports;
}

async function screen(query = "", readOnly = false) {
  const params = new URLSearchParams(query);
  const overrides = {
    "next/navigation": { useRouter: () => ({}), useSearchParams: () => params },
    "@/lib/list-location": await loadTs("../lib/list-location.ts"),
    "@/lib/list-resume": await loadTs("../lib/list-resume.ts"),
    "@/lib/runtime-config": { READ_ONLY: readOnly },
  };
  const reset = await loadTs("../components/filter/ResetSearchFilters.tsx", overrides);
  const { ActiveFilters } = await loadTs("../components/filter/ActiveFilters.tsx", {
    ...overrides, "./ResetSearchFilters": reset,
  });
  const { DealFeed } = await loadTs("../components/deal/DealFeed.tsx", {
    ...overrides,
    "@/components/filter/ResetSearchFilters": reset,
    "./CategoryGrid": { CategoryGrid: () => null },
    "./DealList": { DealList: () => null },
    "./SortBar": { SortBar: () => null },
    "@/lib/api": {},
    "@/lib/format": { formatRelativeTime: () => "방금" },
    "@/lib/ui-events": { SOURCE_VISIBILITY_CHANGED_EVENT: "source-change" },
  });
  return {
    filters: () => renderToStaticMarkup(createElement(ActiveFilters)),
    feed: (loadFailed = false) => renderToStaticMarkup(createElement(DealFeed, {
      deals: [], meta: null, loadFailed, listParams: {}, categories: [],
    })),
  };
}

test("각 단독 필터에서 실제 초기화 버튼을 렌더하고 조건이 없으면 숨긴다", async () => {
  for (const query of ["q=노트북", "shopName=쿠팡", "category=DIGITAL", "sourceId=1", "hideEnded=true"]) {
    assert.match((await screen(query)).filters(), /검색·필터 초기화/);
  }
  assert.equal((await screen()).filters(), "");
});

test("조건 유무에 맞는 빈 결과 안내와 행동을 표시한다", async () => {
  const filtered = (await screen("category=DIGITAL")).feed();
  assert.match(filtered, /선택한 검색·필터 조건에 맞는 핫딜이 없어요/);
  assert.match(filtered, /검색·필터 초기화/);
  const empty = (await screen()).feed();
  assert.match(empty, /현재 표시할 핫딜이 없어요/);
  assert.match(empty, /다시 불러오기/);
  assert.doesNotMatch(empty, /검색·필터 초기화|아직 수집|곧 채워/);
});

test("조회 실패를 빈 결과로 표시하지 않고 재시도를 제공한다", async () => {
  const failed = (await screen("q=노트북")).feed(true);
  assert.match(failed, /딜 목록을 불러오지 못했어요/);
  assert.match(failed, /다시 시도/);
  assert.doesNotMatch(failed, /핫딜이 없어요|검색·필터 초기화/);
});

test("설정 허용 모드에서만 키워드 관리 링크와 저장 설정 안내를 제공한다", async () => {
  const editable = (await screen()).feed();
  assert.match(editable, /href="\/settings\/keywords"/);
  assert.match(editable, /관심·제외 키워드와 출처 표시 설정도 목록에 적용됩니다/);
  const readOnly = (await screen("", true)).feed();
  assert.doesNotMatch(readOnly, /settings\/keywords|설정을 확인해/);
});
