package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NewDealDetailSupportTest {

    private final DealRepository dealRepository = mock(DealRepository.class);
    private final NewDealDetailSupport support = new NewDealDetailSupport(dealRepository);
    private final Source source = source();

    @Test
    @DisplayName("신규 Deal만 설정된 요청 수까지 상세 보강한다")
    void enrichesOnlyNewDealsWithinLimit() {
        given(dealRepository.existsBySourceIdAndExternalId(1L, "existing")).willReturn(true);
        List<String> requested = new ArrayList<>();

        List<CollectedDeal> result = support.enrich(
                source,
                List.of(deal("existing"), deal("new-1"), deal("new-2"), deal("new-3")),
                2,
                item -> {
                    requested.add(item.externalId());
                    return item.withProductInfo("테스트몰", "https://shop.example.com/" + item.externalId());
                }
        );

        assertThat(requested).containsExactly("new-1", "new-2");
        assertThat(result).extracting(CollectedDeal::productUrl)
                .containsExactly(null, "https://shop.example.com/new-1", "https://shop.example.com/new-2", null);
    }

    @Test
    @DisplayName("상세 요청 실패는 해당 Deal의 목록 정보만 유지하고 다음 신규 Deal을 계속 처리한다")
    void isolatesDetailFailure() {
        List<CollectedDeal> result = support.enrich(
                source,
                List.of(deal("failed"), deal("succeeded")),
                2,
                item -> {
                    if ("failed".equals(item.externalId())) {
                        throw new IllegalStateException("detail unavailable");
                    }
                    return item.withProductInfo(null, "https://shop.example.com/succeeded");
                }
        );

        assertThat(result.get(0).productUrl()).isNull();
        assertThat(result.get(1).productUrl()).isEqualTo("https://shop.example.com/succeeded");
    }

    private static Source source() {
        Source source = mock(Source.class);
        given(source.getId()).willReturn(1L);
        given(source.getCode()).willReturn("test-source");
        return source;
    }

    private static CollectedDeal deal(String externalId) {
        return new CollectedDeal(
                externalId, "https://source.example.com/" + externalId, "목록몰", externalId,
                null, null, null, null, false, null);
    }
}
