package com.pickdeal.deal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealGroup;
import com.pickdeal.deal.domain.DealGroupRepository;
import com.pickdeal.deal.domain.DealMatchNormalizer;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DealRetentionServiceTest {

    // 시드 데이터보다 과거로 기준을 잡아 이 테스트가 만든 행만 정리 대상이 되게 한다.
    private static final OffsetDateTime CUTOFF = OffsetDateTime.of(2001, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9));
    private static final OffsetDateTime OLD = CUTOFF.minusDays(1);
    private static final OffsetDateTime RECENT = CUTOFF.plusDays(1);

    @Autowired
    private DealRetentionService retentionService;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private DealGroupRepository dealGroupRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ApplicationContext context;

    private Source sourceA;
    private Source sourceB;

    @BeforeEach
    void setUp() {
        sourceA = sourceRepository.save(new Source("보관 테스트 A", "https://retention-a.example.com", "retention-a", true));
        sourceB = sourceRepository.save(new Source("보관 테스트 B", "https://retention-b.example.com", "retention-b", true));
    }

    @Test
    @DisplayName("그룹이 없는 Deal은 기준보다 오래된 것만 삭제한다")
    void deletesOnlyOldUngroupedDeals() {
        Deal old = saveDeal(sourceA, "old", OLD);
        Deal recent = saveDeal(sourceA, "recent", RECENT);

        DealRetentionService.Result result = retentionService.purgeOlderThan(CUTOFF);

        assertThat(result.deletedDeals()).isEqualTo(1);
        assertThat(result.deletedGroups()).isZero();
        assertThat(dealRepository.existsById(old.getId())).isFalse();
        assertThat(dealRepository.existsById(recent.getId())).isTrue();
    }

    @Test
    @DisplayName("구성원이 모두 오래된 그룹은 그룹과 구성원을 함께 삭제한다")
    void deletesGroupWhenAllMembersAreOld() {
        Deal first = saveDeal(sourceA, "group-old-a", OLD);
        Deal second = saveDeal(sourceB, "group-old-b", OLD.minusDays(3));
        DealGroup group = saveGroup(first, second);

        DealRetentionService.Result result = retentionService.purgeOlderThan(CUTOFF);

        assertThat(result.deletedDeals()).isEqualTo(2);
        assertThat(result.deletedGroups()).isEqualTo(1);
        assertThat(dealGroupRepository.existsById(group.getId())).isFalse();
        assertThat(dealRepository.existsById(first.getId())).isFalse();
        assertThat(dealRepository.existsById(second.getId())).isFalse();
    }

    @Test
    @DisplayName("최근 구성원이 하나라도 있으면 오래된 대표를 포함해 그룹 전체를 유지한다")
    void keepsWholeGroupWhenAnyMemberIsRecent() {
        Deal oldRepresentative = saveDeal(sourceA, "group-mixed-a", OLD);
        Deal recentMember = saveDeal(sourceB, "group-mixed-b", RECENT);
        DealGroup group = saveGroup(oldRepresentative, recentMember);

        DealRetentionService.Result result = retentionService.purgeOlderThan(CUTOFF);

        assertThat(result.deletedDeals()).isZero();
        assertThat(result.deletedGroups()).isZero();
        assertThat(dealGroupRepository.existsById(group.getId())).isTrue();
        assertThat(dealRepository.findById(oldRepresentative.getId()))
                .hasValueSatisfying(deal -> assertThat(deal.getDealGroup().getId()).isEqualTo(group.getId()));
        assertThat(dealRepository.existsById(recentMember.getId())).isTrue();
    }

    @Test
    @DisplayName("보관 기간 정리 스케줄러는 명시적으로 켜기 전에는 등록되지 않는다")
    void schedulerIsDisabledByDefault() {
        assertThat(context.getBeansOfType(DealRetentionScheduler.class)).isEmpty();
    }

    private DealGroup saveGroup(Deal representative, Deal member) {
        DealGroup group = dealGroupRepository.save(new DealGroup(representative));
        representative.joinGroup(group);
        member.joinGroup(group);
        entityManager.flush();
        return group;
    }

    private Deal saveDeal(Source source, String externalId, OffsetDateTime postedAt) {
        String title = "보관 테스트 " + externalId;
        return dealRepository.save(new Deal(
                source,
                title,
                null,
                null,
                null,
                null,
                "KRW",
                "테스트",
                null,
                null,
                null,
                source.getBaseUrl() + "/deals/" + externalId,
                null,
                externalId,
                DealMatchNormalizer.titleHash(title, null),
                DealStatus.ACTIVE,
                postedAt,
                postedAt
        ));
    }
}
