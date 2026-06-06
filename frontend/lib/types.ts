/**
 * [LEGACY/데모 전용] 목데이터(`mock-data.ts`)와 현재 화면이 쓰는 데모 타입.
 *
 * 백엔드 통신 계약은 이제 `api-types.ts`(DealSummary/DealDetail 등)가 단일 출처다.
 * 이 파일의 `Deal`/`Source` 등은 백엔드와 어긋나는 데모 모델이며,
 * 화면을 백엔드 API로 교체하는 단계(3번)에서 컴포넌트를 `api-types.ts`로 옮기고 점진 제거한다.
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
