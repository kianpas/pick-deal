"use client";

import Image from "next/image";
import { ImageOff } from "lucide-react";
import { useState } from "react";

interface Props {
  src: string | null;
  alt: string;
  detail?: boolean;
  ended?: boolean;
}

/** URL이 바뀌면 실패 상태도 초기화한다. 페이지 전체는 Server Component로 유지한다. */
export function DealThumbnail(props: Props) {
  return <ThumbnailImage key={props.src} {...props} />;
}

function ThumbnailImage({ src, alt, detail = false, ended = false }: Props) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <span className="flex h-full w-full flex-col items-center justify-center gap-1 bg-surface-2 text-xs text-fg-muted">
        <ImageOff className="size-5" aria-hidden="true" />
        <span>이미지 없음</span>
      </span>
    );
  }

  return (
    <Image
      src={src}
      alt={alt}
      {...(detail ? { width: 96, height: 96 } : { fill: true })}
      sizes={detail ? "96px" : "(max-width: 640px) 80px, 112px"}
      className={detail ? `h-full w-full object-scale-down ${ended ? "opacity-60" : ""}` : "object-cover"}
      unoptimized
      // 개드립은 외부 Referer가 포함된 이미지 요청을 거부한다.
      referrerPolicy={/^https:\/\/(?:www\.)?dogdrip\.net\//i.test(src) ? "no-referrer" : undefined}
      onError={() => setFailed(true)}
    />
  );
}
