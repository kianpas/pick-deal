package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.deal.dto.DealSummaryResponse;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.domain.SourceVisibility;
import com.pickdeal.source.domain.SourceVisibilityRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DealServiceTest {

    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private DealService dealService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private SourceVisibilityRepository sourceVisibilityRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Test
    @DisplayName("카테고리 목록은 노출 중인 딜(종료 포함)의 카테고리를 중복 없이 정렬해 반환한다")
    void findCategoriesReturnsDistinctSortedVisibleCategories() {
        Source visibleSource = sourceRepository.save(new Source("카테고리테스트출처", "https://cat.example.com", "cat-test", true));
        saveDeal(visibleSource, "cat-1", "테스트카테고리B", DealStatus.ACTIVE);
        saveDeal(visibleSource, "cat-2", "테스트카테고리A", DealStatus.ACTIVE);
        saveDeal(visibleSource, "cat-3", "테스트카테고리B", DealStatus.ACTIVE); // 중복
        saveDeal(visibleSource, "cat-4", null, DealStatus.ACTIVE);              // 카테고리 없음
        saveDeal(visibleSource, "cat-5", "테스트만료카테고리", DealStatus.EXPIRED); // 만료 딜도 목록에 노출된다

        Source hiddenSource = sourceRepository.save(new Source("숨김테스트출처", "https://hidden-cat.example.com", "cat-hidden", true));
        sourceVisibilityRepository.save(new SourceVisibility(DEFAULT_USER_ID, hiddenSource, false));
        saveDeal(hiddenSource, "cat-6", "테스트숨김카테고리", DealStatus.ACTIVE);

        List<String> categories = dealService.findCategories();

        assertThat(categories)
                .contains("테스트카테고리A", "테스트카테고리B", "테스트만료카테고리")
                .doesNotContain("테스트숨김카테고리")
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .isSorted();
    }

    @Test
    @DisplayName("목록은 종료/품절 딜도 상태와 함께 포함한다 (조용히 숨기지 않음)")
    void findDealsIncludesEndedDeals() {
        keywordRepository.deleteAll(); // 시드 관심 키워드가 테스트 딜을 거르지 않도록 비운다

        Source source = sourceRepository.save(new Source("종료테스트출처", "https://ended.example.com", "ended-test", true));
        saveDeal(source, "ended-1", "기타", DealStatus.ACTIVE);
        saveDeal(source, "ended-2", "기타", DealStatus.EXPIRED);
        saveDeal(source, "ended-3", "기타", DealStatus.SOLD_OUT);

        List<DealSummaryResponse> items =
                dealService.findDeals(0, 50, "latest", List.of(source.getId()), null, null).items();

        assertThat(items).hasSize(3);
        assertThat(items).extracting(DealSummaryResponse::status)
                .containsExactlyInAnyOrder("ACTIVE", "EXPIRED", "SOLD_OUT");
    }

    private void saveDeal(Source source, String externalId, String category, DealStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        dealRepository.save(new Deal(
                source, "테스트 딜 " + externalId, null, 1000L, null, null, "KRW",
                category, null, "https://cat.example.com/" + externalId, externalId,
                null, status, now, now
        ));
    }
}
