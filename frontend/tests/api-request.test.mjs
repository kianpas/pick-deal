import { readFile } from "node:fs/promises";
import assert from "node:assert/strict";
import { test } from "node:test";
import ts from "typescript";

async function loadApi({ readOnly }) {
  const source = await readFile(new URL("../lib/api.ts", import.meta.url), "utf8");
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022, esModuleInterop: true },
  });
  const compiled = { exports: {} };
  new Function("require", "module", "exports", outputText)(
    (id) => {
      if (id === "./runtime-config") return { READ_ONLY: readOnly };
      throw new Error(`unexpected import: ${id}`);
    },
    compiled,
    compiled.exports,
  );
  return compiled.exports;
}

function stubFetch(respond) {
  const calls = [];
  const original = globalThis.fetch;
  globalThis.fetch = async (url, init) => {
    calls.push({ url, init });
    return respond(url, init);
  };
  return { calls, restore: () => { globalThis.fetch = original; } };
}

const json = (body, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { "Content-Type": "application/json" },
});

test("GET 요청은 Content-Type을 붙이지 않고 본문이 있는 요청만 JSON 타입을 붙인다", async () => {
  const api = await loadApi({ readOnly: false });
  const fetch = stubFetch((url) => json({ data: url.includes("keywords") ? { id: 1 } : [] }));
  try {
    await api.getSources();
    await api.createKeyword({ keyword: "ssd", type: "INTEREST" });
  } finally {
    fetch.restore();
  }

  const [get, post] = fetch.calls;
  assert.equal(get.init.headers["Content-Type"], undefined);
  assert.equal(post.init.headers["Content-Type"], "application/json");
  assert.ok(get.init.signal instanceof AbortSignal, "모든 요청에 타임아웃 signal을 전달한다");
});

test("JSON이 아닌 오류 응답은 상태 코드를 보존한 ApiError로 바꾼다", async () => {
  const api = await loadApi({ readOnly: false });
  const fetch = stubFetch(() => new Response("<html>Bad Gateway</html>", { status: 502, statusText: "Bad Gateway" }));
  try {
    await assert.rejects(api.getDeal(1), (error) => {
      assert.ok(error instanceof api.ApiError);
      assert.equal(error.status, 502);
      assert.equal(error.code, "INVALID_RESPONSE");
      return true;
    });
  } finally {
    fetch.restore();
  }
});

test("요청 시간 초과는 TIMEOUT ApiError로 바꾼다", async () => {
  const api = await loadApi({ readOnly: false });
  const fetch = stubFetch(() => { throw new DOMException("timed out", "TimeoutError"); });
  try {
    await assert.rejects(api.getDeals(), (error) => {
      assert.ok(error instanceof api.ApiError);
      assert.equal(error.status, 504);
      assert.equal(error.code, "TIMEOUT");
      return true;
    });
  } finally {
    fetch.restore();
  }
});

test("조회 전용 모드에서만 카테고리·판매처 목록을 서버 캐시하고 딜·출처는 캐시하지 않는다", async () => {
  for (const readOnly of [true, false]) {
    const api = await loadApi({ readOnly });
    const fetch = stubFetch(() => json({ data: [], meta: { page: 0, size: 20, totalElements: 0, totalPages: 0, hasNext: false } }));
    try {
      await api.getDealCategories();
      await api.getDealShops();
      await api.getDeals();
      await api.getSources();
    } finally {
      fetch.restore();
    }

    const [categories, shops, deals, sources] = fetch.calls.map((call) => call.init.next);
    const expected = readOnly ? { revalidate: 300 } : undefined;
    assert.deepEqual(categories, expected);
    assert.deepEqual(shops, expected);
    assert.equal(deals, undefined);
    assert.equal(sources, undefined);
  }
});
