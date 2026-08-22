package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PagedCollectionSupportTest {

    @Test
    @DisplayName("고유 항목 상한을 채우면 남은 페이지를 요청하지 않는다")
    void stopsAtMaxUniqueItems() {
        List<Integer> requestedPages = new ArrayList<>();

        List<CollectedDeal> deals = PagedCollectionSupport.collect(
                3,
                2,
                page -> {
                    requestedPages.add(page);
                    return page == 1 ? "a" : "a,b,c";
                },
                html -> List.of(html.split(",")),
                PagedCollectionSupportTest::deal
        );

        assertThat(deals).extracting(CollectedDeal::externalId).containsExactly("a", "b");
        assertThat(requestedPages).containsExactly(1, 2);
    }

    private static CollectedDeal deal(String externalId) {
        return new CollectedDeal(
                externalId, "https://example.com/" + externalId, null, externalId,
                null, null, null, null, false, null);
    }
}
