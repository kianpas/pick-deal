package com.pickdeal.collector.support;

/** 상세 페이지 전용 영역에서 확인한 판매몰 정보. 확인할 수 없는 필드는 nullable이다. */
public record CollectedProductInfo(
        String shopName,
        String productUrl
) {
}
