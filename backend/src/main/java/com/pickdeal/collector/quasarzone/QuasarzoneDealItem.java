package com.pickdeal.collector.quasarzone;

/**
 * 퀘이사존 핫딜 목록 페이지에서 파싱한 딜 1건. (docs/05 A.2의 Parse 단계 산출물)
 */
public record QuasarzoneDealItem(
        String externalId,
        String url,
        String storeName,
        String title,
        java.math.BigDecimal price,
        String category,
        String thumbnailUrl,
        Integer commentCount,
        boolean ended,
        String postedAtText,
        String currency
) {
    public QuasarzoneDealItem(String externalId, String url, String storeName, String title, Number price,
            String category, String thumbnailUrl, Integer commentCount, boolean ended, String postedAtText) {
        this(externalId, url, storeName, title, price == null ? null : new java.math.BigDecimal(price.toString()),
                category, thumbnailUrl, commentCount, ended, postedAtText, "KRW");
    }
}
