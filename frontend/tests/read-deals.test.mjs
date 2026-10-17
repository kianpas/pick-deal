import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { test, beforeEach } from "node:test";
import ts from "typescript";

async function loadModule(path) {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } });
  return import(`data:text/javascript;base64,${Buffer.from(outputText).toString("base64")}`);
}
const { READ_DEALS_STORAGE_KEY, READ_DEALS_CHANGED_EVENT, isDealRead, markDealRead, parseReadDeals } =
  await loadModule("../lib/read-deals.ts");

const DAY = 24 * 60 * 60 * 1000;
const NOW = Date.UTC(2026, 9, 17);
let store;
let events;
beforeEach(() => {
  store = new Map();
  events = [];
  globalThis.localStorage = {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => store.set(key, value),
  };
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
});

test("상세를 연 딜은 딜 ID와 그룹 ID로 기록해 대표가 바뀐 그룹 카드도 읽음으로 본다", () => {
  markDealRead({ id: 10, groupId: 3 }, NOW);
  const read = parseReadDeals(store.get(READ_DEALS_STORAGE_KEY), NOW);

  assert.equal(isDealRead(read, { id: 10, groupId: 3 }), true);
  assert.equal(isDealRead(read, { id: 11, groupId: 3 }), true, "같은 그룹의 다른 대표");
  assert.equal(isDealRead(read, { id: 10, groupId: null }), true, "그룹이 풀린 같은 딜");
  assert.equal(isDealRead(read, { id: 12, groupId: null }), false);
  assert.deepEqual(events, [READ_DEALS_CHANGED_EVENT]);
});

test("90일이 지났거나 형식이 잘못된 기록은 버린다", () => {
  const raw = JSON.stringify({
    "d:1": NOW - DAY,
    "d:2": NOW - 91 * DAY,
    "d:3": NOW + DAY,
    "x:4": NOW,
    "d:0": NOW,
    "d:5": "yesterday",
  });
  assert.deepEqual([...parseReadDeals(raw, NOW).keys()], ["d:1"]);
  for (const broken of [null, "", "not json", "[]", "null", "42"]) {
    assert.equal(parseReadDeals(broken, NOW).size, 0);
  }
});

test("기록은 최근 2000건까지만 유지한다", () => {
  const entries = Object.fromEntries(Array.from({ length: 2000 }, (_, i) => [`d:${i + 1}`, NOW - DAY - i]));
  store.set(READ_DEALS_STORAGE_KEY, JSON.stringify(entries));

  markDealRead({ id: 9999, groupId: null }, NOW);
  const read = parseReadDeals(store.get(READ_DEALS_STORAGE_KEY), NOW);

  assert.equal(read.size, 2000);
  assert.equal(read.has("d:9999"), true);
  assert.equal(read.has("d:2000"), false, "가장 오래된 기록을 버린다");
});

test("저장소가 차단돼도 예외 없이 읽음 기록만 생략한다", () => {
  globalThis.localStorage = {
    getItem: () => { throw new Error("blocked"); },
    setItem: () => { throw new Error("blocked"); },
  };
  assert.doesNotThrow(() => markDealRead({ id: 1, groupId: null }, NOW));
  assert.deepEqual(events, []);
});
