package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
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

    @Test
    @DisplayName("카테고리 목록은 노출 중인 딜의 카테고리를 중복 없이 정렬해 반환한다")
    void findCategoriesReturnsDistinctSortedVisibleCategories() {
        Source visibleSource = sourceRepository.save(new Source("카테고리테스트출처", "https://cat.example.com", "cat-test", true));
        saveDeal(visibleSource, "cat-1", "테스트카테고리B", DealStatus.ACTIVE);
        saveDeal(visibleSource, "cat-2", "테스트카테고리A", DealStatus.ACTIVE);
        saveDeal(visibleSource, "cat-3", "테스트카테고리B", DealStatus.ACTIVE); // 중복
        saveDeal(visibleSource, "cat-4", null, DealStatus.ACTIVE);              // 카테고리 없음
        saveDeal(visibleSource, "cat-5", "테스트만료카테고리", DealStatus.EXPIRED); // 만료 딜

        Source hiddenSource = sourceRepository.save(new Source("숨김테스트출처", "https://hidden-cat.example.com", "cat-hidden", true));
        sourceVisibilityRepository.save(new SourceVisibility(DEFAULT_USER_ID, hiddenSource, false));
        saveDeal(hiddenSource, "cat-6", "테스트숨김카테고리", DealStatus.ACTIVE);

        List<String> categories = dealService.findCategories();

        assertThat(categories)
                .contains("테스트카테고리A", "테스트카테고리B")
                .doesNotContain("테스트만료카테고리", "테스트숨김카테고리")
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .isSorted();
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
