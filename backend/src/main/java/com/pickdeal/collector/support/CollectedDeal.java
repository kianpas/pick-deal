package com.pickdeal.collector.support;

import java.time.OffsetDateTime;

/**
 * 출처별 파싱 결과를 표준 형태로 정규화한 딜 1건 (파이프라인의 normalize 산출물).
 * 출처마다 없는 정보가 있어(예: 루리웹은 썸네일 없음) 대부분 필드가 nullable이다.
 *
 * @param postedAt 게시 시각. 해석 불가하면 null — 저장 시 수집 시각으로 대체된다.
 */
public record CollectedDeal(
        String externalId,
        String url,
        String storeName,
        String title,
        Long price,
        String category,
        Integer commentCount,
        String thumbnailUrl,
        boolean ended,
        OffsetDateTime postedAt
) {

    /** 출처에 게시된 원문 제목("[판매처] 상품명"). 판매처가 없으면 제목 그대로. */
    public String rawTitle() {
        return storeName != null ? "[" + storeName + "] " + title : title;
    }
}
