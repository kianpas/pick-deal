package com.pickdeal.collector.quasarzone;

/**
 * 퀘이사존 핫딜 목록 페이지에서 파싱한 딜 1건. (docs/05 A.2의 Parse 단계 산출물)
 */
public record QuasarzoneDealItem(
        String externalId,
        String url,
        String storeName,
        String title,
        Long price,
        String category,
        String thumbnailUrl,
        boolean ended,
        String postedAtText
) {
}
