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
      <span className={`flex w-full flex-col items-center justify-center gap-1 bg-surface-2 text-xs text-fg-muted ${detail ? "aspect-video" : "h-full"}`}>
        <ImageOff className="size-5" aria-hidden="true" />
        <span>이미지 없음</span>
      </span>
    );
  }

  return (
    <Image
      src={src}
      alt={alt}
      {...(detail ? { width: 768, height: 432 } : { fill: true })}
      sizes={detail ? "(max-width: 768px) 100vw, 768px" : "(max-width: 640px) 80px, 112px"}
      className={detail ? `h-auto w-full object-contain ${ended ? "opacity-60" : ""}` : "object-cover"}
      unoptimized
      // 개드립은 외부 Referer가 포함된 이미지 요청을 거부한다.
      referrerPolicy={/^https:\/\/(?:www\.)?dogdrip\.net\//i.test(src) ? "no-referrer" : undefined}
      onError={() => setFailed(true)}
    />
  );
}
