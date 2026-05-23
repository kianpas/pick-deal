/**
 * Frontend 도메인 타입.
 * 백엔드 DTO와 이름·필드를 정렬해 추후 API 연동 시 매핑 부담을 최소화한다.
 * 참고: 일부 필드는 MVP 범위(docs/01 §3.1)를 넘어선 확장 데모용이다.
 */

export type SourceId =
  | "ruliweb"
  | "fmkorea"
  | "quasarzone"
  | "reddit"
  | "slickdeals";

export type ShopId =
  | "coupang"
  | "naver"
  | "11st"
  | "gmarket"
  | "ssg"
  | "himart"
  | "lotteon"
  | "auction"
  | "iherb"
  | "amazon";

export type CategoryId =
  | "all"
  | "appliance"
  | "digital"
  | "computer"
  | "living"
  | "food"
  | "fashion"
  | "sports"
  | "book";

export type SortOption =
  | "recommend"
  | "popular"
  | "latest"
  | "comments"
  | "price-asc"
  | "price-desc";

export interface Source {
  id: SourceId;
  name: string;
  enabled: boolean;
}

export interface ShopCount {
  id: ShopId | "all";
  name: string;
  count: number;
}

export interface Deal {
  id: string;
  source: SourceId;
  sourceName: string;
  shop: ShopId;
  shopName: string;
  category: string;
  subCategory: string;
  title: string;
  thumbnail: string;
  price: number;
  originalPrice: number;
  discountPercent: number;
  currency: "KRW" | "USD";
  originalUrl: string;
  postedAt: string;
  voteCount: number;
  commentCount: number;
  isHot: boolean;
  freeShipping: boolean;
  shippingNote?: string; // "로켓배송", "$40 이상 무료배송" 등
}

export interface BestItem {
  rank: number;
  shopName: string;
  title: string;
  commentCount: number;
}

export interface KeywordTag {
  keyword: string;
  count: number;
}

export interface AISummary {
  body: string; // 자연어 본문
  highlights: string[]; // 강조할 키워드
}
