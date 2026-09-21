"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useTransition } from "react";

export function ShopFilter({ shops, selected, failed = false }: {
  shops: string[];
  selected: string[];
  failed?: boolean;
}) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [pending, startTransition] = useTransition();
  // 목록에서 사라진 선택값도 해제할 수 있도록 남긴다.
  const options = [...new Set([...shops, ...selected])];

  function update(next: string[]) {
    const params = new URLSearchParams(searchParams.toString());
    params.delete("shopName");
    params.delete("page");
    next.forEach((name) => params.append("shopName", name));
    startTransition(() => router.push(`${pathname}${params.size ? `?${params}` : ""}`, { scroll: false }));
  }

  return (
    <fieldset disabled={pending} aria-busy={pending} className="min-w-0 space-y-1">
      <legend className="mb-2 text-sm font-semibold">쇼핑몰</legend>
      <p className="text-xs text-fg-muted">여러 곳 선택 가능 · 저장된 이름 기준</p>
      {failed && <p role="status" className="text-xs text-warning">쇼핑몰 목록을 불러오지 못했습니다.</p>}
      {failed && <button type="button" onClick={() => startTransition(() => router.refresh())}
        className="min-h-11 px-3 text-sm text-brand">다시 시도</button>}
      <button type="button" aria-pressed={selected.length === 0} onClick={() => update([])}
        className="min-h-11 w-full rounded-lg px-3 text-left text-sm hover:bg-surface disabled:opacity-50">
        {selected.length === 0 ? "✓ 전체 쇼핑몰" : "전체 쇼핑몰 · 선택 해제"}
      </button>
      {options.map((name) => (
        <label key={name} className="flex min-h-11 cursor-pointer items-center gap-2 rounded-lg px-3 py-2 text-sm hover:bg-surface">
          <input type="checkbox" checked={selected.includes(name)}
            onChange={() => update(selected.includes(name) ? selected.filter((value) => value !== name) : [...selected, name])}
            className="size-4 shrink-0 accent-brand" />
          <span className="min-w-0 break-words">{name}</span>
        </label>
      ))}
      {!failed && options.length === 0 && <p className="py-2 text-xs text-fg-muted">수집된 쇼핑몰이 아직 없습니다.</p>}
      <span role="status" className="sr-only">{pending ? "필터 적용 중" : `${selected.length}개 쇼핑몰 선택`}</span>
    </fieldset>
  );
}
