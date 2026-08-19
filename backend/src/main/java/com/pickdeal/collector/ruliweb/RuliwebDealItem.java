package com.pickdeal.collector.ruliweb;

/**
 * 루리웹 핫딜 게시판 목록에서 파싱한 딜 1건. (docs/05 A.2의 Parse 단계 산출물)
 *
 * <p>퀘이사존과 달리 가격·썸네일이 구조화돼 있지 않다 — 가격은 제목 끝 관례
 * ("... / 730900원")에서 추출하고, 목록에는 썸네일이 없다.
 */
public record RuliwebDealItem(
        String externalId,
        String url,
        String storeName,
        String title,
        Long price,
        String category,
        boolean ended,
        String postedAtText
) {
}
