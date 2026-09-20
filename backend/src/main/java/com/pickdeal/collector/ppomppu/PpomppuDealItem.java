package com.pickdeal.collector.ppomppu;

/** 목록에서 관측한 값만 보관한다. 상품 URL·종료 상태·시각 정규화는 아직 다루지 않는다. */
public record PpomppuDealItem(
        String externalId, String url, String storeName, String title, Long price,
        String category, Integer commentCount, String thumbnailUrl, String postedAtText) {
}
