package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DealUpsertGroupingTest {

    @Test
    void oversizedFieldsDoNotPreventOtherDealsFromBeingSaved() {
        Source source = upsertSupport.findOrRegisterSource("length-test", "길이 검사", "https://example.com");
        var now = OffsetDateTime.now();
        var oversizedTitle = new CollectedDeal("bad", "https://example.com/1", null,
                "가".repeat(301), 1L, null, null, null, false, now);
        var oversizedOptional = new CollectedDeal("optional", "https://example.com/2", "몰".repeat(101),
                "정상 제목", 1L, "가".repeat(51), null, "x".repeat(1001), false, now,
                "https://example.com/" + "x".repeat(2000));
        int saved = upsertSupport.upsertAll(source, List.of(oversizedTitle, oversizedOptional, collected("ok")), now);
        dealRepository.flush();
        assertThat(saved).isEqualTo(2);
        assertThat(dealRepository.existsBySourceIdAndExternalId(source.getId(), "bad")).isFalse();
        Deal optional = dealRepository.findBySourceIdAndExternalId(source.getId(), "optional").orElseThrow();
        assertThat(optional.getProductUrl()).isNull();
        assertThat(optional.getShopName()).isNull();
        assertThat(optional.getCategory()).isNull();
        assertThat(optional.getThumbnailUrl()).isNull();
    }

    @Test
    void oversizedRequiredLinksAndComposedTitlesAreSkipped() {
        Source source = upsertSupport.findOrRegisterSource("required-length", "필수 길이 검사", "https://example.com");
        var now = OffsetDateTime.now();
        var longId = new CollectedDeal("x".repeat(201), "https://example.com", null, "제목", 1L, null, null, null, false, now);
        var longUrl = new CollectedDeal("url", "x".repeat(1001), null, "제목", 1L, null, null, null, false, now);
        var longComposedTitle = new CollectedDeal("title", "https://example.com", "몰", "가".repeat(300), 1L, null, null, null, false, now);
        assertThat(upsertSupport.upsertAll(source, List.of(longId, longUrl, longComposedTitle), now)).isZero();
        dealRepository.flush();
    }

    @Autowired
    private DealUpsertSupport upsertSupport;

    @Autowired
    private DealRepository dealRepository;

    @Test
    @DisplayName("수집 저장 경로에서 다른 출처의 강한 일치 Deal을 자동 그룹화한다")
    void groupsNewDealsDuringUpsert() {
        Source firstSource = upsertSupport.findOrRegisterSource(
                "upsert-group-a", "Upsert 그룹 A", "https://upsert-a.example.com");
        Source secondSource = upsertSupport.findOrRegisterSource(
                "upsert-group-b", "Upsert 그룹 B", "https://upsert-b.example.com");
        OffsetDateTime now = OffsetDateTime.now();

        upsertSupport.upsertAll(firstSource, List.of(collected("a-1")), now);
        upsertSupport.upsertAll(secondSource, List.of(collected("b-1")), now);

        Deal first = dealRepository.findBySourceIdAndExternalId(firstSource.getId(), "a-1").orElseThrow();
        Deal second = dealRepository.findBySourceIdAndExternalId(secondSource.getId(), "b-1").orElseThrow();
        assertThat(first.getTitleNormHash()).isNotNull();
        assertThat(second.getDealGroup()).isSameAs(first.getDealGroup()).isNotNull();
    }

    @Test
    @DisplayName("기존 null 제목 해시는 재수집 때 채우고 두 출처가 모두 관측되면 그룹화한다")
    void backfillsMatchKeyOnRecollection() {
        Source firstSource = upsertSupport.findOrRegisterSource(
                "backfill-group-a", "Backfill 그룹 A", "https://backfill-a.example.com");
        Source secondSource = upsertSupport.findOrRegisterSource(
                "backfill-group-b", "Backfill 그룹 B", "https://backfill-b.example.com");
        OffsetDateTime now = OffsetDateTime.now();
        Deal first = dealRepository.save(legacyDeal(firstSource, "legacy-a", now));
        Deal second = dealRepository.save(legacyDeal(secondSource, "legacy-b", now));

        upsertSupport.upsertAll(firstSource, List.of(collected("legacy-a")), now.plusMinutes(1));
        assertThat(first.getTitleNormHash()).isNotNull();
        assertThat(first.getDealGroup()).isNull();

        upsertSupport.upsertAll(secondSource, List.of(collected("legacy-b")), now.plusMinutes(1));
        assertThat(second.getTitleNormHash()).isNotNull();
        assertThat(second.getDealGroup()).isSameAs(first.getDealGroup()).isNotNull();
    }

    private static CollectedDeal collected(String externalId) {
        return new CollectedDeal(
                externalId,
                "https://source.example.com/deals/" + externalId,
                "쿠팡",
                "삼성 SSD 1TB",
                99_000L,
                "PC/하드웨어",
                null,
                null,
                false,
                OffsetDateTime.now()
        );
    }

    private static Deal legacyDeal(Source source, String externalId, OffsetDateTime now) {
        return new Deal(
                source,
                "[쿠팡] 삼성 SSD 1TB",
                null,
                99_000L,
                null,
                null,
                "KRW",
                "PC/하드웨어",
                "쿠팡",
                null,
                null,
                source.getBaseUrl() + "/deals/" + externalId,
                null,
                externalId,
                null,
                DealStatus.ACTIVE,
                now,
                now
        );
    }
}
