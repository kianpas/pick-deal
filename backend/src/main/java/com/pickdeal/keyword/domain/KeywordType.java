package com.pickdeal.keyword.domain;

/**
 * 키워드 종류. 딜 필터링 규칙에 직결된다(docs/01 §3.2).
 */
public enum KeywordType {
    /** 관심 키워드: 하나 이상 등록되면, 이를 포함하는 딜만 노출. */
    INTEREST,
    /** 제외 키워드: 제목/본문에 포함되면 딜을 제외. */
    EXCLUDE
}
