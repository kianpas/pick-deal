package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealGroupRepository;
import com.pickdeal.deal.domain.DealMatchNormalizer;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DealGroupingServiceTest {

    @Autowired
    private DealGroupingService groupingService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private DealGroupRepository dealGroupRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Test
    @DisplayName("다른 출처의 정규화 제목·판매몰·가격이 모두 같으면 원본을 보존하고 그룹화한다")
    void groupsExactTitleShopAndPriceAcrossSources() {
        Source firstSource = saveSource("group-source-a");
        Source secondSource = saveSource("group-source-b");
        long beforeCount = dealRepository.count();

        Deal first = saveDeal(firstSource, "a-1", "[쿠팡] 삼성 SSD 1TB", "쿠팡", 99_000L, null, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "b-1", "삼성 SSD 1 TB", "쿠팡", 99_000L, null, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNotNull();
        assertThat(second.getDealGroup()).isSameAs(first.getDealGroup());
        assertThat(dealRepository.count()).isEqualTo(beforeCount + 2);
        assertThat(dealGroupRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("상품 URL·판매처·가격이 같으면 정보가 풍부한 Deal을 대표로 선택한다")
    void groupsExactTitleAndProductUrl() {
        Source firstSource = saveSource("url-source-a");
        Source secondSource = saveSource("url-source-b");
        String productUrl = "https://shop.example.com/products/123";

        Deal first = saveDeal(firstSource, "url-a", "[테스트몰] 키보드 K1", "테스트몰",
                50_000L, productUrl, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "url-b", "키보드 K1", "테스트몰",
                50_000L, productUrl, "https://images.example.com/k1.jpg");
        groupingService.groupIfMatched(second);

        assertThat(second.getDealGroup()).isSameAs(first.getDealGroup());
        assertThat(second.getDealGroup().getRepresentativeDeal()).isSameAs(second);
    }

    @Test
    @DisplayName("제목 표현이 서로 달라도 상품 URL이 같으면 그룹화한다")
    void groupsByProductUrlEvenWhenTitlesDiffer() {
        Source firstSource = saveSource("url-only-a");
        Source secondSource = saveSource("url-only-b");
        String productUrl = "https://shop.example.com/products/9850";

        // 출처마다 작성자가 다르므로 제목이 같게 정규화되는 일은 드물다.
        // 상품 URL은 그보다 강한 근거이므로 제목 일치를 전제하지 않아야 한다.
        Deal first = saveDeal(firstSource, "uo-a", "[알리] AMD 라이젠7 9850X3D 멀티팩 정품", "알리",
                648_013L, productUrl, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "uo-b", "[알리/국내정품] 9850X3D (멀티팩)", "알리",
                648_013L, productUrl, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getTitleNormHash()).isNotEqualTo(second.getTitleNormHash());
        assertThat(second.getDealGroup()).isNotNull();
        assertThat(second.getDealGroup()).isSameAs(first.getDealGroup());
    }

    @Test
    @DisplayName("상품 URL이 같아도 판매처가 다르면 자동 그룹화하지 않는다")
    void rejectsSameProductUrlWithDifferentShop() {
        Source firstSource = saveSource("url-shop-a");
        Source secondSource = saveSource("url-shop-b");
        String sharedLandingUrl = "https://event.example.com/summer-sale";

        Deal first = saveDeal(firstSource, "us-a", "서로 다른 상품 A", "판매몰A",
                10_000L, sharedLandingUrl, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "us-b", "서로 다른 상품 B", "판매몰B",
                10_000L, sharedLandingUrl, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNull();
        assertThat(second.getDealGroup()).isNull();
    }

    @Test
    @DisplayName("상품 URL과 판매처가 같아도 가격이 다르면 자동 그룹화하지 않는다")
    void rejectsSameProductUrlWithDifferentPrice() {
        Source firstSource = saveSource("url-price-a");
        Source secondSource = saveSource("url-price-b");
        String sharedLandingUrl = "https://event.example.com/summer-sale";

        Deal first = saveDeal(firstSource, "up-a", "서로 다른 상품 A", "판매몰",
                10_000L, sharedLandingUrl, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "up-b", "서로 다른 상품 B", "판매몰",
                20_000L, sharedLandingUrl, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNull();
        assertThat(second.getDealGroup()).isNull();
    }

    @Test
    @DisplayName("용량이 다르면 제목이 비슷해도 그룹화하지 않는다")
    void keepsDifferentCapacitySeparate() {
        Source firstSource = saveSource("capacity-source-a");
        Source secondSource = saveSource("capacity-source-b");

        Deal first = saveDeal(firstSource, "capacity-a", "[쿠팡] 삼성 SSD 1TB", "쿠팡",
                99_000L, null, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "capacity-b", "[쿠팡] 삼성 SSD 2TB", "쿠팡",
                99_000L, null, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNull();
        assertThat(second.getDealGroup()).isNull();
    }

    @Test
    @DisplayName("같은 제목이어도 판매몰과 상품 URL 근거가 없으면 그룹화하지 않는다")
    void rejectsWeakTitleOnlyMatch() {
        Source firstSource = saveSource("weak-source-a");
        Source secondSource = saveSource("weak-source-b");

        Deal first = saveDeal(firstSource, "weak-a", "공통 행사 상품", "판매몰A",
                10_000L, null, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(secondSource, "weak-b", "공통 행사 상품", "판매몰B",
                10_000L, null, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNull();
        assertThat(second.getDealGroup()).isNull();
    }

    @Test
    @DisplayName("같은 출처의 별도 게시글은 교차 출처 그룹 대상으로 삼지 않는다")
    void ignoresSameSourcePosts() {
        Source source = saveSource("same-source");

        Deal first = saveDeal(source, "same-a", "[쿠팡] 동일 상품", "쿠팡", 10_000L, null, null);
        groupingService.groupIfMatched(first);
        Deal second = saveDeal(source, "same-b", "[쿠팡] 동일 상품", "쿠팡", 10_000L, null, null);
        groupingService.groupIfMatched(second);

        assertThat(first.getDealGroup()).isNull();
        assertThat(second.getDealGroup()).isNull();
    }

    @Test
    @DisplayName("다른 출처에 강한 후보가 둘 이상이면 출처별 대표를 추측하지 않고 건너뛴다")
    void rejectsMultipleCandidatesFromSameSource() {
        Source firstSource = saveSource("ambiguous-source-a");
        Source secondSource = saveSource("ambiguous-source-b");

        Deal first = saveDeal(firstSource, "ambiguous-a1", "[쿠팡] 동일 상품", "쿠팡",
                10_000L, null, null);
        Deal duplicate = saveDeal(firstSource, "ambiguous-a2", "[쿠팡] 동일 상품", "쿠팡",
                10_000L, null, null);
        Deal incoming = saveDeal(secondSource, "ambiguous-b", "[쿠팡] 동일 상품", "쿠팡",
                10_000L, null, null);

        groupingService.groupIfMatched(incoming);

        assertThat(first.getDealGroup()).isNull();
        assertThat(duplicate.getDealGroup()).isNull();
        assertThat(incoming.getDealGroup()).isNull();
    }

    private Source saveSource(String code) {
        return sourceRepository.save(new Source(
                "테스트 출처 " + code,
                "https://" + code + ".example.com",
                code,
                true
        ));
    }

    private Deal saveDeal(
            Source source,
            String externalId,
            String title,
            String shopName,
            Long price,
            String productUrl,
            String thumbnailUrl
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        return dealRepository.save(new Deal(
                source,
                title,
                null,
                price,
                null,
                null,
                "KRW",
                "테스트",
                shopName,
                null,
                thumbnailUrl,
                source.getBaseUrl() + "/deals/" + externalId,
                productUrl,
                externalId,
                DealMatchNormalizer.titleHash(title, shopName),
                DealStatus.ACTIVE,
                now,
                now
        ));
    }
}
