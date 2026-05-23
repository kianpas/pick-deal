export function formatPrice(value: number, currency: "KRW" | "USD"): string {
  if (currency === "USD") {
    return `${value.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}$`;
  }
  return `${value.toLocaleString("ko-KR")}원`;
}
