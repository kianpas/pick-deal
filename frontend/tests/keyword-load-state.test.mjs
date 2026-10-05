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

test("키워드 조회 실패는 빈 목록·편집 폼 대신 재시도를 표시하고 재조회 성공 시 목록을 복구한다", async (t) => {
  t.mock.method(console, "error", () => {});
  let response = new Error("unavailable");
  const api = { getKeywords: async () => { if (response instanceof Error) throw response; return response; } };
  const navigation = { useRouter: () => ({ refresh() {} }) };
  const manager = await loadTs("../components/settings/KeywordManager.tsx", { "@/lib/api": api });
  const error = await loadTs("../components/settings/KeywordLoadError.tsx", { "next/navigation": navigation });
  const { default: Page } = await loadTs("../app/settings/keywords/page.tsx", {
    "@/lib/api": api,
    "@/lib/runtime-config": { READ_ONLY: false },
    "@/components/layout/TopBar": { TopBar: () => null },
    "@/components/settings/KeywordManager": manager,
    "@/components/settings/KeywordLoadError": error,
  });
  let html = renderToStaticMarkup(await Page());
  assert.match(html, /키워드를 불러오지 못했어요/);
  assert.match(html, /다시 시도/);
  assert.doesNotMatch(html, /등록된 키워드가 없습니다|<form/);
  response = [{ id: 1, keyword: "노트북", type: "INTEREST" }];
  html = renderToStaticMarkup(await Page());
  assert.match(html, /노트북/);
  assert.match(html, /<form/);
  assert.doesNotMatch(html, /키워드를 불러오지 못했어요/);
  response = [];
  html = renderToStaticMarkup(await Page());
  assert.match(html, /등록된 키워드가 없습니다/);
  assert.doesNotMatch(html, /키워드를 불러오지 못했어요/);
});

test("재시도는 서버 화면을 갱신하고 대기 중에는 중복 클릭을 막는다", async () => {
  let refreshes = 0;
  let pending = false;
  const { KeywordLoadError } = await loadTs("../components/settings/KeywordLoadError.tsx", {
    react: { useTransition: () => [pending, (action) => action()] },
    "next/navigation": { useRouter: () => ({ refresh: () => refreshes++ }) },
  });
  const button = KeywordLoadError().props.children.find((node) => node.type === "button");
  button.props.onClick();
  assert.equal(refreshes, 1);
  pending = true;
  const waiting = KeywordLoadError();
  assert.equal(waiting.props.children.find((node) => node.type === "button").props.disabled, true);
  assert.match(renderToStaticMarkup(waiting), /키워드를 다시 불러오는 중입니다/);
});

test("쇼핑몰 빈 목록은 수집 상태를 단정하지 않고 실패 안내와 구분한다", async () => {
  const { ShopFilter } = await loadTs("../components/filter/ShopFilter.tsx", {
    "next/navigation": {
      useRouter: () => ({}), usePathname: () => "/", useSearchParams: () => new URLSearchParams(),
    },
  });
  const render = (failed) => renderToStaticMarkup(createElement(ShopFilter, { shops: [], selected: [], failed }));
  assert.match(render(false), /표시할 쇼핑몰이 없습니다/);
  assert.doesNotMatch(render(false), /수집된 쇼핑몰|불러오지 못했습니다/);
  assert.match(render(true), /쇼핑몰 목록을 불러오지 못했습니다/);
  assert.doesNotMatch(render(true), /표시할 쇼핑몰이 없습니다/);
});
