/** 목록 복귀·필터 등 개인 탐색 상태를 포함하지 않는 픽딜 상세 주소. */
export function dealShareUrl(currentUrl: string, dealId: number): string {
  return new URL(`/deals/${dealId}`, currentUrl).href;
}

export async function copyDealShareLink(
  currentUrl: string,
  dealId: number,
  clipboard: Pick<Clipboard, "writeText"> | undefined,
): Promise<{ url: string; copied: boolean }> {
  const url = dealShareUrl(currentUrl, dealId);
  try {
    if (!clipboard) return { url, copied: false };
    await clipboard.writeText(url);
    return { url, copied: true };
  } catch {
    return { url, copied: false };
  }
}
