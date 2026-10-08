const FILTER_KEYS = ["q", "category", "sourceId", "shopName", "hideEnded"] as const;

/** 켜진 탐색 조건의 종류 수(검색어·카테고리·쇼핑몰·커뮤니티·종료 숨기기). 페이지·복귀 표식은 필터가 아니다. */
export function countSearchFilters(params: URLSearchParams): number {
  return ["q", "category", "shopName"].filter((key) => params.getAll(key).some((value) => value.trim())).length
    + (params.getAll("sourceId").some((value) => /^[1-9]\d*$/.test(value) && Number.isSafeInteger(Number(value))) ? 1 : 0)
    + (params.get("hideEnded") === "true" ? 1 : 0);
}

/** 실제 탐색 조건만 판별한다. */
export function hasSearchFilters(params: URLSearchParams): boolean {
  return countSearchFilters(params) > 0;
}

/** 탐색 조건과 이전 페이지·위치만 지운다. 저장된 사용자 설정은 변경하지 않는다. */
export function resetSearchLocation(params: URLSearchParams): string {
  const next = new URLSearchParams(params);
  for (const key of [...FILTER_KEYS, "page", "resume"]) next.delete(key);
  return next.size ? `/?${next}` : "/";
}

/** 목록 복귀에는 허용된 필터와 복귀 대상 ID만 전달한다. 외부 URL은 허용하지 않는다. */
export function listLocation(value?: string, includeResume = true): string {
  if (!value || (value !== "/" && !value.startsWith("/?"))) return "/";
  const input = new URLSearchParams(value.slice(2));
  const output = new URLSearchParams();
  for (const key of FILTER_KEYS) {
    for (const item of input.getAll(key)) {
      if (item) output.append(key, item);
    }
  }
  const resume = input.get("resume");
  if (includeResume && resume && /^[1-9]\d*$/.test(resume) && Number.isSafeInteger(Number(resume))) {
    output.set("resume", resume);
  }
  return output.size ? `/?${output}` : "/";
}
