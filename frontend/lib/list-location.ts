/** 목록 복귀에는 허용된 필터만 전달한다. 외부 URL이나 다른 경로는 허용하지 않는다. */
export function listLocation(value?: string): string {
  if (!value || (value !== "/" && !value.startsWith("/?"))) return "/";
  const input = new URLSearchParams(value.slice(2));
  const output = new URLSearchParams();
  for (const key of ["q", "category", "sourceId", "shopName"]) {
    for (const item of input.getAll(key)) {
      if (item) output.append(key, item);
    }
  }
  return output.size ? `/?${output}` : "/";
}
