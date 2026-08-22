package com.pickdeal.collector.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;

/** 출처 공통의 페이지·항목 상한과 실행 내 중복 제거를 적용한다. */
public final class PagedCollectionSupport {

    private PagedCollectionSupport() {
    }

    public static <T> List<CollectedDeal> collect(
            int maxPages,
            int maxItems,
            IntFunction<String> fetchPage,
            Function<String, List<T>> parse,
            Function<T, CollectedDeal> normalize
    ) {
        Map<String, CollectedDeal> deals = new LinkedHashMap<>();

        for (int page = 1; page <= maxPages && deals.size() < maxItems; page++) {
            for (T item : parse.apply(fetchPage.apply(page))) {
                CollectedDeal deal = normalize.apply(item);
                deals.putIfAbsent(deal.externalId(), deal);
                if (deals.size() == maxItems) {
                    break;
                }
            }
        }

        return List.copyOf(deals.values());
    }
}
