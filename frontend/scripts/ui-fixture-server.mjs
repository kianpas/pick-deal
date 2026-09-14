// UI 검증 전용 API. 운영 DB/수집기에 접근하지 않는다.
// node scripts/ui-fixture-server.mjs
// frontend의 NEXT_PUBLIC_API_BASE_URL=http://127.0.0.1:18081 로 실행한다.
import { createServer } from "node:http";

const sourcePosts = [1, 2].map((id) => ({
  dealId: id, sourceId: id, sourceName: id === 1 ? "퀘이사존" : "다른 커뮤니티",
  originalUrl: "https://example.com/post", productUrl: "https://example.com/product",
  status: "ACTIVE", postedAt: "2026-09-13T12:00:00Z", commentCount: id * 12,
}));
const deals = [
  ["[판매처] 아주 긴 한국어 상품 제목 무선 헤드폰 노이즈 캔슬링 특별 구성 추가 사은품 포함 한정 행사", 1234567],
  ["SuperLongProductModelNumberWithoutSpacesABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 999999999],
  ["무료 다운로드 상품", 0],
  ["가격 정보가 없는 상품", null],
].map(([title, price], i) => ({
  id: i + 1, title, price, currency: "KRW", originalPrice: null, discountRate: null,
  category: "전자기기", shopName: i === 1 ? "아주긴판매처이름ABCDEFGHIJKLMNOPQRSTUVWXYZ" : "판매처",
  sourceNames: ["퀘이사존", "다른 커뮤니티"], sourceCount: 2, sourceId: 1,
  sourceName: "퀘이사존", commentCount: 12, status: i === 3 ? "SOLD_OUT" : "ACTIVE",
  thumbnailUrl: null, postedAt: "2026-09-13T12:00:00Z", collectedAt: "2026-09-13T12:00:00Z",
  sourcePosts, originalUrl: "https://example.com/post", productUrl: "https://example.com/product",
  description: "모바일 UI 검증용 데이터입니다. 실제 판매 정보가 아닙니다.",
}));

createServer((req, res) => {
  const url = new URL(req.url, "http://127.0.0.1");
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  if (req.method === "OPTIONS") { res.writeHead(204).end(); return; }
  if (req.method !== "GET") { res.writeHead(403).end('{}'); return; }
  let body;
  if (url.pathname === "/api/v1/deals/categories") {
    body = { data: ["전자기기", "식품", "생활용품", "게임", "패션", "아주 긴 카테고리 이름"] };
  } else if (url.pathname === "/api/v1/deals") {
    const items = deals.filter(d => !url.searchParams.get("q") || d.title.includes(url.searchParams.get("q")));
    body = { data: items, meta: { page: 0, size: 20, totalElements: items.length, totalPages: 1, hasNext: false } };
  } else if (/^\/api\/v1\/deals\/\d+$/.test(url.pathname)) {
    const deal = deals.find(d => d.id === Number(url.pathname.split('/').at(-1)));
    if (!deal) { res.writeHead(404).end('{}'); return; }
    body = { data: deal };
  } else if (url.pathname === "/api/v1/sources") {
    body = { data: sourcePosts.map(p => ({ id: p.sourceId, name: p.sourceName, visible: true })) };
  } else if (url.pathname === "/api/v1/keywords") {
    body = { data: [] };
  } else { res.writeHead(404).end('{}'); return; }
  res.end(JSON.stringify(body));
}).listen(18081, "127.0.0.1", () => console.log("UI fixture API: http://127.0.0.1:18081 (test only)"));
