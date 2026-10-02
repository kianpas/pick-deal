"use client";

import { Link2 } from "lucide-react";
import { useId, useRef, useState } from "react";
import { copyDealShareLink } from "@/lib/deal-share";

export function DealShareButton({ dealId }: { dealId: number }) {
  const id = useId();
  const busy = useRef(false);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");
  const [manualUrl, setManualUrl] = useState("");

  async function copyLink() {
    if (busy.current) return;
    busy.current = true;
    setPending(true);
    setMessage("");
    const result = await copyDealShareLink(window.location.href, dealId, navigator.clipboard);
    setManualUrl(result.copied ? "" : result.url);
    setMessage(result.copied ? "픽딜 링크를 복사했습니다." : "자동 복사가 안 됩니다. 아래 주소를 선택해 복사해 주세요.");
    setPending(false);
    busy.current = false;
  }

  return (
    <div className="ms-auto min-w-0 max-w-full space-y-2">
      <div className="flex flex-col items-end gap-1">
        <button
          type="button"
          onClick={copyLink}
          aria-disabled={pending}
          aria-busy={pending}
          className="inline-flex min-h-11 touch-manipulation items-center justify-center gap-1.5 rounded-lg border border-border bg-surface px-3 text-sm font-medium text-fg transition hover:border-border-strong"
        >
          <Link2 className="size-4 shrink-0" aria-hidden="true" />
          {pending ? "복사 중…" : "링크 복사"}
        </button>
        <p id={`${id}-status`} role="status" aria-atomic="true" className="min-w-0 max-w-xs text-end text-xs text-fg-muted">
          {message}
        </p>
      </div>
      {manualUrl && (
        <div className="space-y-1">
          <label htmlFor={`${id}-url`} className="block text-xs text-fg-muted">공유할 픽딜 주소</label>
          <input
            id={`${id}-url`}
            type="url"
            readOnly
            value={manualUrl}
            aria-describedby={`${id}-status`}
            onFocus={(event) => event.currentTarget.select()}
            className="min-h-11 w-full min-w-0 rounded-lg border border-border bg-surface px-3 text-sm text-fg"
          />
        </div>
      )}
    </div>
  );
}
