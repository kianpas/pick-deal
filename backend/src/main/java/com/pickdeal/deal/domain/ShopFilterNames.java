package com.pickdeal.deal.domain;

import java.util.Locale;
import java.util.Map;

/** 조회 옵션과 필터 전용 별칭. 저장된 원문과 상품 그룹 판정은 변경하지 않는다. */
public final class ShopFilterNames {
    private static final Map<String, String> ALIASES = Map.of(
            "카카오쇼핑", "카카오쇼핑", "카카오톡딜", "카카오쇼핑",
            "카카오 톡딜", "카카오쇼핑",
            "g마켓", "지마켓", "지마켓", "지마켓",
            "네이버", "네이버", "네이버쇼핑", "네이버");

    private ShopFilterNames() {}

    public static String canonical(String name) {
        if (name == null || name.isBlank()) return null;
        return ALIASES.getOrDefault(name.toLowerCase(Locale.ROOT), name);
    }
}
