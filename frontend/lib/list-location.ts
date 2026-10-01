/** 목록 복귀에는 허용된 필터와 복귀 대상 ID만 전달한다. 외부 URL은 허용하지 않는다. */
export function listLocation(value?: string, includeResume = true): string {
  if (!value || (value !== "/" && !value.startsWith("/?"))) return "/";
  const input = new URLSearchParams(value.slice(2));
  const output = new URLSearchParams();
  for (const key of ["q", "category", "sourceId", "shopName", "hideEnded"]) {
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
