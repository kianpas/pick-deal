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
const format = await loadTs("../lib/format.ts");
const { DealCard } = await loadTs("../components/deal/DealCard.tsx", {
  "@/lib/format": format,
  "@/components/deal/DealThumbnail": { DealThumbnail: () => null },
});

function render(commentCount, status = "ACTIVE", sourceCount = 1) {
  const deal = {
    id: 1, title: "테스트 상품", price: 10000, originalPrice: null, discountRate: null,
    currency: "KRW", shopName: null, thumbnailUrl: null, commentCount, status,
    sourceCount, sourceNames: ["테스트 출처"], postedAt: "2026-10-03T00:00:00Z",
  };
  return renderToStaticMarkup(createElement(DealCard, { deal, showThumbnail: false }));
}

function comments(markup) {
  return markup.match(/<span class="inline-flex items-center gap-1" aria-label="댓글 \d+개">([\s\S]*?)<\/span>/)?.[1];
}

test("진행 중 상품은 댓글 20개부터 숫자만 굵게 표시한다", () => {
  for (const count of [0, 19, 20, 21]) {
    const markup = comments(render(count));
    assert.equal(markup.includes('class="font-bold"'), count >= 20);
    assert.match(markup, /class="lucide lucide-message-circle size-3"/);
    assert.doesNotMatch(markup, /text-brand|text-danger|bg-/);
  }
});

test("종료·품절 상품은 댓글이 많아도 강조하지 않는다", () => {
  for (const status of ["EXPIRED", "SOLD_OUT"]) {
    assert.doesNotMatch(comments(render(30, status)), /font-bold/);
  }
});

test("댓글 미확인은 숨기고 그룹도 전달된 대표 댓글 수만 사용한다", () => {
  assert.equal(comments(render(null)), undefined);
  assert.doesNotMatch(comments(render(19, "ACTIVE", 3)), /font-bold/);
  assert.match(comments(render(20, "ACTIVE", 3)), /font-bold/);
});
