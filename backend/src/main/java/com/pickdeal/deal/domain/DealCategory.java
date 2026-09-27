package com.pickdeal.deal.domain;

import java.util.Map;

/** 조회용 분류. 저장된 출처 카테고리는 변경하지 않는다. */
public enum DealCategory {
    PC("PC/하드웨어"), DIGITAL("디지털/가전"), FOOD("식품/건강"),
    LIVING("생활/가구"), FASHION("패션/잡화"), GAME("게임/SW"),
    BENEFIT("상품권/혜택"), ETC("기타");

    private final String label;

    DealCategory(String label) { this.label = label; }
    public String label() { return label; }

    private static final Map<String, DealCategory> COMMON = Map.ofEntries(
            Map.entry("PC/하드웨어", PC), Map.entry("PC", PC),
            Map.entry("가전/TV", DIGITAL), Map.entry("가전/가구", DIGITAL),
            Map.entry("노트북/모바일", DIGITAL), Map.entry("디지털", DIGITAL), Map.entry("전자", DIGITAL), Map.entry("전자제품", DIGITAL),
            Map.entry("식품", FOOD), Map.entry("식품/건강", FOOD),
            Map.entry("생활용품", LIVING), Map.entry("생활", LIVING), Map.entry("가구", LIVING),
            Map.entry("의류", FASHION), Map.entry("의류/잡화", FASHION), Map.entry("패션/의류", FASHION),
            Map.entry("게임/SW", GAME), Map.entry("게임S/W", GAME), Map.entry("게임H/W", GAME),
            Map.entry("모바일/상품권", BENEFIT), Map.entry("상품권/쿠폰", BENEFIT), Map.entry("포인트/래플", BENEFIT),
            Map.entry("기타", ETC));

    // 복합 분류는 출처별로 명시한다. 제목 추측으로 개별 상품을 재분류하지 않는다.
    private static final Map<String, Map<String, DealCategory>> BY_SOURCE = Map.of(
            "quasarzone", Map.of("생활/식품", FOOD));

    public static DealCategory from(String sourceCode, String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) return ETC;
        String raw = rawCategory.trim();
        var sourceMapping = sourceCode == null ? Map.<String, DealCategory>of()
                : BY_SOURCE.getOrDefault(sourceCode, Map.of());
        return sourceMapping.getOrDefault(raw, COMMON.getOrDefault(raw, ETC));
    }

    public static DealCategory from(Deal deal) {
        return from(deal.getSource().getCode(), deal.getCategory());
    }
}
