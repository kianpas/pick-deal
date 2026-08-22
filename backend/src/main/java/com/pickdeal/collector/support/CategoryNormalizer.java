package com.pickdeal.collector.support;

import java.util.Map;

/** 출처 카테고리의 확실한 표기 차이만 PickDeal의 대표 문자열로 통일한다. */
public final class CategoryNormalizer {

    private static final Map<String, String> ALIASES = Map.of(
            "게임S/W", "게임/SW"
    );

    private CategoryNormalizer() {
    }

    /** 정확히 등록된 별칭만 변환한다. 미등록 값은 원문을 보존한다. */
    public static String normalize(String rawCategory) {
        if (rawCategory == null) {
            return null;
        }
        String category = rawCategory.trim();
        return ALIASES.getOrDefault(category, category);
    }
}
