import { Store } from "lucide-react";
import type { ShopCount } from "@/lib/types";

const SHOP_STYLE: Record<string, { bg: string; label: string }> = {
  all: { bg: "bg-surface-2 text-fg-muted", label: "▦" },
  coupang: { bg: "bg-red-500 text-white", label: "C" },
  naver: { bg: "bg-emerald-500 text-white", label: "N" },
  "11st": { bg: "bg-rose-500 text-white", label: "11" },
  gmarket: { bg: "bg-yellow-400 text-zinc-900", label: "G" },
  ssg: { bg: "bg-zinc-900 text-yellow-300 ring-1 ring-zinc-700", label: "SSG" },
  himart: { bg: "bg-red-600 text-white", label: "H" },
  lotteon: { bg: "bg-orange-500 text-white", label: "ON" },
  auction: { bg: "bg-rose-600 text-white", label: "A" },
  iherb: { bg: "bg-emerald-600 text-white", label: "iH" },
  amazon: { bg: "bg-yellow-500 text-zinc-900", label: "a" },
};

export function ShopIcon({ id, size = 24 }: { id: ShopCount["id"] | string; size?: number }) {
  const style = SHOP_STYLE[id];
  const px = `${size}px`;
  if (!style) {
    return (
      <span
        className="grid shrink-0 place-items-center rounded-md bg-surface-2 text-fg-muted"
        style={{ width: px, height: px }}
      >
        <Store className="size-3.5" />
      </span>
    );
  }
  return (
    <span
      className={`grid shrink-0 place-items-center rounded-md text-[10px] font-semibold ${style.bg}`}
      style={{ width: px, height: px }}
      aria-hidden
    >
      {style.label}
    </span>
  );
}
