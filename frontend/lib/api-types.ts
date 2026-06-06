/**
 * 백엔드 API 계약 타입. 백엔드 DTO(`com.pickdeal.*.dto`)를 1:1로 미러한다.
 * 이 파일이 프론트-백 통신의 단일 출처다 — 데모용 `types.ts`(Deal 등)와 혼동 금지.
 *
 * 명명 규칙: 백엔드 `XxxResponse` → 프론트 `Xxx`(예: DealSummaryResponse → DealSummary).
 * 시각 필드는 ISO-8601 문자열(KST, 예: "2026-05-20T18:10:00+09:00").
 */

// ---- 공통 응답 봉투 (ApiResponse<T>) ----

/** 페이지네이션 메타 (PageMetaResponse). */
export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/** 에러 봉투 본문 (ErrorResponse). */
export interface ApiErrorBody {
  code: string;
  message: string;
  details?: string[];
}

/**
 * 모든 응답의 봉투. 성공 시 `data`(+목록이면 `meta`), 실패 시 `error`만 채워진다
 * (백엔드가 NON_NULL 직렬화로 빈 필드를 생략).
 */
export interface ApiEnvelope<T> {
  data?: T;
  meta?: PageMeta;
  error?: ApiErrorBody;
}

// ---- 딜 (Deal) ----

/** 딜 상태 (백엔드 DealStatus). */
export type DealStatus = "ACTIVE" | "SOLD_OUT" | "EXPIRED";

/**
 * 목록 응답 항목 (DealSummaryResponse). 카드 렌더에 필요한 요약 필드.
 * nullable 필드는 `| null`로 표기(백엔드가 null을 보낼 수 있음).
 */
export interface DealSummary {
  id: number;
  title: string;
  price: number | null;
  originalPrice: number | null;
  discountRate: number | null;
  currency: string;
  category: string | null;
  thumbnailUrl: string | null;
  sourceId: number;
  sourceName: string;
  postedAt: string;
  status: DealStatus;

  // ---- 수집기 단계 추출 검토 · 현재 백엔드 미제공(전부 optional) ----
  // 백엔드가 실제로 내려주기 시작하면 그때 위 계약 블록으로 승격한다.
  shop?: string;
  isHot?: boolean;
  freeShipping?: boolean;
  shippingNote?: string;
  voteCount?: number;
  commentCount?: number;
}

/** 상세 응답 (DealDetailResponse) = 요약 + 본문/원문/외부ID/수집시각. */
export interface DealDetail extends DealSummary {
  description: string | null;
  originalUrl: string;
  externalId: string;
  collectedAt: string;
}

// ---- 출처 (Source) ----

/** 출처 + 현재 사용자의 표시 상태 (SourceResponse). */
export interface SourceItem {
  id: number;
  name: string;
  baseUrl: string;
  visible: boolean;
}

/** PATCH /sources/{id}/visibility 요청 본문 (UpdateSourceVisibilityRequest). */
export interface UpdateSourceVisibilityRequest {
  visible: boolean;
}

// ---- 키워드 (Keyword) ----

export type KeywordType = "INTEREST" | "EXCLUDE";

/** 키워드 항목 (KeywordResponse). */
export interface KeywordItem {
  id: number;
  keyword: string;
  type: KeywordType;
  createdAt: string;
}

/** POST /keywords 요청 본문 (CreateKeywordRequest). */
export interface CreateKeywordRequest {
  keyword: string;
  type: KeywordType;
}
