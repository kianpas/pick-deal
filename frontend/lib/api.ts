/**
 * 백엔드 API 클라이언트. `NEXT_PUBLIC_API_BASE_URL` 기반 fetch 래퍼.
 *
 * 응답 봉투({ data, meta } | { error })를 벗겨 호출부엔 알맹이만 돌려준다.
 * 에러 봉투/비정상 상태면 {@link ApiError}를 던진다.
 */

import type {
  ApiEnvelope,
  CreateKeywordRequest,
  DealDetail,
  DealSummary,
  KeywordItem,
  KeywordType,
  PageMeta,
  SourceItem,
} from "./api-types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/** 백엔드 에러 봉투 또는 비정상 HTTP 상태를 표현하는 예외. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<ApiEnvelope<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  // 204 No Content(키워드 삭제 등) 등 본문 없는 응답.
  if (response.status === 204) {
    return {};
  }

  const body = (await response.json()) as ApiEnvelope<T>;
  if (!response.ok || body.error) {
    throw new ApiError(
      response.status,
      body.error?.code ?? "UNKNOWN",
      body.error?.message ?? response.statusText,
    );
  }
  return body;
}

// ---- 딜 ----

export interface DealListParams {
  page?: number;
  size?: number;
  sort?: "latest" | "discount";
  sourceId?: number[];
  category?: string;
  q?: string;
}

export interface DealListResult {
  items: DealSummary[];
  meta: PageMeta;
}

/** GET /api/v1/deals — 핫딜 목록(필터/정렬/페이지). */
export async function getDeals(params: DealListParams = {}): Promise<DealListResult> {
  const search = new URLSearchParams();
  if (params.page != null) search.set("page", String(params.page));
  if (params.size != null) search.set("size", String(params.size));
  if (params.sort) search.set("sort", params.sort);
  if (params.category) search.set("category", params.category);
  if (params.q) search.set("q", params.q);
  params.sourceId?.forEach((id) => search.append("sourceId", String(id)));

  const query = search.toString();
  const envelope = await request<DealSummary[]>(`/api/v1/deals${query ? `?${query}` : ""}`);
  return { items: envelope.data ?? [], meta: envelope.meta as PageMeta };
}

/** GET /api/v1/deals/categories — 노출 중인 딜의 카테고리 목록(중복 없음, 정렬). */
export async function getDealCategories(): Promise<string[]> {
  const envelope = await request<string[]>("/api/v1/deals/categories");
  return envelope.data ?? [];
}

/** GET /api/v1/deals/{id} — 핫딜 상세. */
export async function getDeal(id: number): Promise<DealDetail> {
  const envelope = await request<DealDetail>(`/api/v1/deals/${id}`);
  return envelope.data as DealDetail;
}

// ---- 출처 ----

/** GET /api/v1/sources — 출처 목록 + 표시 상태. */
export async function getSources(): Promise<SourceItem[]> {
  const envelope = await request<SourceItem[]>("/api/v1/sources");
  return envelope.data ?? [];
}

/** PATCH /api/v1/sources/{id}/visibility — 출처 표시/숨김. */
export async function updateSourceVisibility(id: number, visible: boolean): Promise<SourceItem> {
  const envelope = await request<SourceItem>(`/api/v1/sources/${id}/visibility`, {
    method: "PATCH",
    body: JSON.stringify({ visible }),
  });
  return envelope.data as SourceItem;
}

// ---- 키워드 ----

/** GET /api/v1/keywords — 키워드 목록(type 생략 시 전체). */
export async function getKeywords(type?: KeywordType): Promise<KeywordItem[]> {
  const query = type ? `?type=${type}` : "";
  const envelope = await request<KeywordItem[]>(`/api/v1/keywords${query}`);
  return envelope.data ?? [];
}

/** POST /api/v1/keywords — 키워드 등록. */
export async function createKeyword(input: CreateKeywordRequest): Promise<KeywordItem> {
  const envelope = await request<KeywordItem>("/api/v1/keywords", {
    method: "POST",
    body: JSON.stringify(input),
  });
  return envelope.data as KeywordItem;
}

/** DELETE /api/v1/keywords/{id} — 키워드 삭제. */
export async function deleteKeyword(id: number): Promise<void> {
  await request<void>(`/api/v1/keywords/${id}`, { method: "DELETE" });
}
