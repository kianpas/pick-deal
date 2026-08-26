package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealGroup;
import com.pickdeal.deal.domain.DealGroupRepository;
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
    private DealGroupRepository dealGroupRepository;

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

    @Test
    @DisplayName("목록과 상세 응답은 출처 게시글의 댓글 수를 그대로 제공한다")
    void responsesExposeSourcePostCommentCount() {
        keywordRepository.deleteAll();
        Source source = sourceRepository.save(new Source("댓글테스트출처", "https://comments.example.com", "comments-test", true));
        Deal saved = saveDeal(source, "comments-1", "기타", DealStatus.ACTIVE, 7);

        DealSummaryResponse summary = dealService
                .findDeals(0, 20, "latest", List.of(source.getId()), null, null)
                .items().get(0);

        assertThat(summary.commentCount()).isEqualTo(7);
        assertThat(dealService.findDeal(saved.getId()).commentCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("목록은 판매몰 이름을, 상세는 원문과 상품 URL을 함께 제공한다")
    void responsesExposeShopAndBothLinks() {
        keywordRepository.deleteAll();
        Source source = sourceRepository.save(
                new Source("링크테스트출처", "https://source.example.com", "links-test", true));
        OffsetDateTime now = OffsetDateTime.now();
        Deal saved = dealRepository.save(new Deal(
                source, "[테스트몰] 링크 테스트 딜", null, 1000L, null, null, "KRW",
                "기타", "테스트몰", null, null,
                "https://source.example.com/deals/1", "https://shop.example.com/products/1",
                "links-1", null, DealStatus.ACTIVE, now, now
        ));

        DealSummaryResponse summary = dealService
                .findDeals(0, 20, "latest", List.of(source.getId()), null, null)
                .items().get(0);

        assertThat(summary.shopName()).isEqualTo("테스트몰");
        assertThat(dealService.findDeal(saved.getId()))
                .satisfies(detail -> {
                    assertThat(detail.shopName()).isEqualTo("테스트몰");
                    assertThat(detail.originalUrl()).isEqualTo("https://source.example.com/deals/1");
                    assertThat(detail.productUrl()).isEqualTo("https://shop.example.com/products/1");
                });
    }

    @Test
    @DisplayName("같은 그룹의 교차 출처 딜은 목록 한 건으로 접고 그룹 단위로 페이지 수를 계산한다")
    void findDealsCollapsesCrossSourceGroupBeforePagination() {
        keywordRepository.deleteAll();
        Source firstSource = sourceRepository.save(
                new Source("그룹출처A", "https://group-a.example.com", "group-list-a", true));
        Source secondSource = sourceRepository.save(
                new Source("그룹출처B", "https://group-b.example.com", "group-list-b", true));
        Deal first = saveDeal(firstSource, "group-list-1", "기타", DealStatus.ACTIVE, 3);
        Deal second = saveDeal(secondSource, "group-list-2", "기타", DealStatus.EXPIRED, 9);
        joinGroup(first, second);

        var response = dealService.findDeals(
                0, 1, "latest", List.of(firstSource.getId(), secondSource.getId()), null, null);

        assertThat(response.items()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(first.getId());
            assertThat(summary.groupId()).isNotNull();
            assertThat(summary.sourceCount()).isEqualTo(2);
            assertThat(summary.sourceNames()).containsExactly("그룹출처A", "그룹출처B");
            assertThat(summary.status()).isEqualTo("ACTIVE");
            assertThat(summary.commentCount()).isEqualTo(3); // 출처별 반응은 합산하지 않는다.
        });
        assertThat(response.meta().totalElements()).isEqualTo(1);
        assertThat(response.meta().totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("출처 필터로 대표 출처가 빠지면 남은 출처를 목록 대표로 사용한다")
    void sourceFilterChoosesRepresentativeFromEligibleMembers() {
        keywordRepository.deleteAll();
        Source firstSource = sourceRepository.save(
                new Source("필터출처A", "https://filter-a.example.com", "group-filter-a", true));
        Source secondSource = sourceRepository.save(
                new Source("필터출처B", "https://filter-b.example.com", "group-filter-b", true));
        Deal first = saveDeal(firstSource, "group-filter-1", "기타", DealStatus.ACTIVE);
        Deal second = saveDeal(secondSource, "group-filter-2", "기타", DealStatus.ACTIVE);
        joinGroup(first, second);

        DealSummaryResponse summary = dealService
                .findDeals(0, 20, "latest", List.of(secondSource.getId()), null, null)
                .items().get(0);

        assertThat(summary.id()).isEqualTo(second.getId());
        assertThat(summary.sourceCount()).isEqualTo(1);
        assertThat(summary.sourceNames()).containsExactly("필터출처B");
    }

    @Test
    @DisplayName("그룹 딜 상세는 출처별 원문과 댓글 수를 각각 제공한다")
    void groupedDetailExposesSourcePostsWithoutSummingComments() {
        keywordRepository.deleteAll();
        Source firstSource = sourceRepository.save(
                new Source("상세출처A", "https://detail-a.example.com", "group-detail-a", true));
        Source secondSource = sourceRepository.save(
                new Source("상세출처B", "https://detail-b.example.com", "group-detail-b", true));
        Deal first = saveDeal(firstSource, "group-detail-1", "기타", DealStatus.ACTIVE, 4);
        Deal second = saveDeal(secondSource, "group-detail-2", "기타", DealStatus.ACTIVE, 11);
        DealGroup group = joinGroup(first, second);

        var detail = dealService.findDeal(first.getId());

        assertThat(detail.groupId()).isEqualTo(group.getId());
        assertThat(detail.sourcePosts()).hasSize(2);
        assertThat(detail.sourcePosts()).extracting(post -> post.sourceName())
                .containsExactlyInAnyOrder("상세출처A", "상세출처B");
        assertThat(detail.sourcePosts()).extracting(post -> post.commentCount())
                .containsExactlyInAnyOrder(4, 11);
        assertThat(detail.sourcePosts()).extracting(post -> post.originalUrl())
                .containsExactlyInAnyOrder(
                        "https://cat.example.com/group-detail-1",
                        "https://cat.example.com/group-detail-2"
                );
    }

    private DealGroup joinGroup(Deal representative, Deal member) {
        DealGroup group = dealGroupRepository.save(new DealGroup(representative));
        representative.joinGroup(group);
        member.joinGroup(group);
        dealRepository.flush();
        return group;
    }

    private Deal saveDeal(Source source, String externalId, String category, DealStatus status) {
        return saveDeal(source, externalId, category, status, null);
    }

    private Deal saveDeal(Source source, String externalId, String category, DealStatus status, Integer commentCount) {
        OffsetDateTime now = OffsetDateTime.now();
        return dealRepository.save(new Deal(
                source, "테스트 딜 " + externalId, null, 1000L, null, null, "KRW",
                category, null, commentCount, null, "https://cat.example.com/" + externalId, null, externalId,
                null, status, now, now
        ));
    }
}
