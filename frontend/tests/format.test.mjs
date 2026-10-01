import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { test } from "node:test";
import ts from "typescript";

const source = await readFile(new URL("../lib/format.ts", import.meta.url), "utf8");
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext } });
const { formatPrice } = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`);

test("USD를 환산 없이 달러·센트로 표시하고 KRW 표시는 유지한다", () => {
  assert.equal(formatPrice(382.76, "USD"), "US$382.76");
  assert.equal(formatPrice(25, "USD"), "US$25.00");
  assert.equal(formatPrice(1234.56, "USD"), "US$1,234.56");
  assert.equal(formatPrice(8550, "KRW"), "8,550원");
  assert.equal(formatPrice(0, "KRW"), "0원");
});
