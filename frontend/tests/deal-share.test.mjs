import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { test } from "node:test";
import ts from "typescript";

const source = await readFile(new URL("../lib/deal-share.ts", import.meta.url), "utf8");
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } });
const { dealShareUrl, copyDealShareLink } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`);

test("공유 주소는 픽딜 상세 링크이며 복귀 필터·해시를 포함하지 않는다", () => {
  assert.equal(dealShareUrl("https://pick-deal.vercel.app/deals/609?returnTo=%2F%3FsourceId%3D3#price", 609), "https://pick-deal.vercel.app/deals/609");
});

test("로컬·미리보기에서도 현재 사이트 주소를 사용한다", () => {
  assert.equal(dealShareUrl("http://localhost:3000/deals/1", 609), "http://localhost:3000/deals/609");
  assert.equal(dealShareUrl("https://preview.vercel.app/deals/1", 609), "https://preview.vercel.app/deals/609");
});

test("클립보드에 정리된 주소를 복사한다", async () => {
  let written;
  const result = await copyDealShareLink("https://pick-deal.vercel.app/deals/609?returnTo=%2F", 609, {
    async writeText(value) { written = value; },
  });
  assert.deepEqual(result, { url: "https://pick-deal.vercel.app/deals/609", copied: true });
  assert.equal(written, result.url);
});

test("클립보드 미지원·권한 거부는 수동 복사용 주소를 반환한다", async () => {
  for (const clipboard of [undefined, { async writeText() { throw new Error("NotAllowedError"); } }]) {
    assert.deepEqual(await copyDealShareLink("https://pick-deal.vercel.app/deals/609", 609, clipboard), {
      url: "https://pick-deal.vercel.app/deals/609", copied: false,
    });
  }
});
