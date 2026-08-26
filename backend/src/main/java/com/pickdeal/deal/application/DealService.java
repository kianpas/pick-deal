package com.pickdeal.deal.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.common.response.PageMetaResponse;
import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealMatchNormalizer;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.deal.dto.CreateDealRequest;
import com.pickdeal.deal.dto.DealDetailResponse;
import com.pickdeal.deal.dto.DealListResponse;
import com.pickdeal.deal.dto.DealSummaryResponse;
import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.keyword.domain.Keyword;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DealService {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_CURRENCY = "KRW";

    private final DealRepository dealRepository;
    private final SourceRepository sourceRepository;
    private final KeywordRepository keywordRepository;
    private final DealGroupingService dealGroupingService;

    // MVP 한정: ACTIVE 딜 전체를 메모리로 올린 뒤 필터/정렬/페이지네이션한다.
    // 키워드 필터가 제목·본문 부분일치라 DB로 내리기 애매한 점을 감안한 소규모 시드 전용 구현.
    // 데이터가 커지면 DB 쿼리/키셋 페이지네이션으로 전환한다(docs/03 §5).
    @Transactional(readOnly = true)
    public DealListResponse findDeals(int page, int size, String sort, List<Long> sourceIds, String category, String query) {
        List<Keyword> excludeKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.EXCLUDE);
        List<Keyword> interestKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.INTEREST);

        List<Deal> sourceEligibleDeals = dealRepository.findVisibleDeals(DEFAULT_USER_ID).stream()
                .filter(deal -> sourceIds == null || sourceIds.isEmpty() || sourceIds.contains(deal.getSource().getId()))
                .toList();

        List<DealGroupView> filteredGroups = groupDeals(sourceEligibleDeals).stream()
                .filter(group -> group.members().stream().anyMatch(deal -> matchesCategory(deal, category)))
                .filter(group -> group.members().stream().anyMatch(deal -> matchesQuery(deal, query)))
                .filter(group -> group.members().stream().noneMatch(deal -> containsAnyKeyword(deal, excludeKeywords)))
                .filter(group -> interestKeywords.isEmpty()
                        || group.members().stream().anyMatch(deal -> containsAnyKeyword(deal, interestKeywords)))
                .sorted(groupComparator(sort))
                .toList();

        int fromIndex = Math.min(page * size, filteredGroups.size());
        int toIndex = Math.min(fromIndex + size, filteredGroups.size());

        List<DealSummaryResponse> items = filteredGroups.subList(fromIndex, toIndex).stream()
                .map(this::toSummary)
                .toList();

        int totalPages = filteredGroups.isEmpty() ? 0 : (int) Math.ceil((double) filteredGroups.size() / size);
        boolean hasNext = page + 1 < totalPages;

        return new DealListResponse(items, new PageMetaResponse(page, size, filteredGroups.size(), totalPages, hasNext));
    }

    /**
     * 노출 중인(출처 표시, 종료 포함) 딜의 카테고리 목록. 중복 제거 후 정렬.
     * 카테고리는 출처가 준 자유 문자열이라(docs/03 §2.1) 실데이터에서 목록을 만든다.
     */
    @Transactional(readOnly = true)
    public List<String> findCategories() {
        return dealRepository.findVisibleDeals(DEFAULT_USER_ID).stream()
                .map(Deal::getCategory)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public DealDetailResponse findDeal(Long dealId) {
        Deal deal = dealRepository.findByIdWithSource(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));

        List<Deal> sourceDeals = deal.getDealGroup() == null
                ? List.of(deal)
                : dealRepository.findByDealGroupIdWithSource(deal.getDealGroup().getId());
        return DealDetailResponse.from(deal, sourceDeals);
    }

    @Transactional
    public DealDetailResponse createDeal(CreateDealRequest request) {
        Source source = sourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + request.sourceId()));

        String externalId = request.externalId().trim();
        if (dealRepository.existsBySourceIdAndExternalId(source.getId(), externalId)) {
            throw new DuplicateResourceException("Deal already exists for source and externalId");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String title = request.title().trim();
        String shopName = normalizeNullable(request.shopName());
        Deal deal = dealRepository.save(new Deal(
                source,
                title,
                normalizeNullable(request.description()),
                request.price(),
                request.originalPrice(),
                request.discountRate(),
                normalizeCurrency(request.currency()),
                normalizeNullable(request.category()),
                shopName,
                null,
                normalizeNullable(request.thumbnailUrl()),
                request.originalUrl().trim(),
                normalizeNullable(request.productUrl()),
                externalId,
                DealMatchNormalizer.titleHash(title, shopName),
                DealStatus.ACTIVE,
                request.postedAt(),
                now
        ));
        dealGroupingService.groupIfMatched(deal);

        return DealDetailResponse.from(deal);
    }

    private boolean matchesCategory(Deal deal, String category) {
        if (!StringUtils.hasText(category)) {
            return true;
        }
        return deal.getCategory() != null && deal.getCategory().equalsIgnoreCase(category.trim());
    }

    private boolean matchesQuery(Deal deal, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        return contains(deal.getTitle(), normalizedQuery) || contains(deal.getDescription(), normalizedQuery);
    }

    private boolean containsAnyKeyword(Deal deal, List<Keyword> keywords) {
        return keywords.stream()
                .map(Keyword::getKeyword)
                .anyMatch(keyword -> contains(deal.getTitle(), keyword) || contains(deal.getDescription(), keyword));
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private List<DealGroupView> groupDeals(List<Deal> deals) {
        Map<String, List<Deal>> grouped = new LinkedHashMap<>();
        for (Deal deal : deals) {
            String key = deal.getDealGroup() == null
                    ? "deal:" + deal.getId()
                    : "group:" + deal.getDealGroup().getId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(deal);
        }

        return grouped.values().stream()
                .map(members -> new DealGroupView(selectRepresentative(members), List.copyOf(members)))
                .toList();
    }

    private Deal selectRepresentative(List<Deal> members) {
        Deal configuredRepresentative = members.get(0).getDealGroup() == null
                ? null
                : members.get(0).getDealGroup().getRepresentativeDeal();
        if (configuredRepresentative != null
                && members.stream().anyMatch(member -> member.getId().equals(configuredRepresentative.getId()))) {
            return configuredRepresentative;
        }

        return members.stream()
                .max(Comparator.comparingInt(this::representativeScore)
                        .thenComparing(this::dealSortTime))
                .orElseThrow();
    }

    private int representativeScore(Deal deal) {
        int score = 0;
        if (StringUtils.hasText(deal.getProductUrl())) {
            score += 4;
        }
        if (deal.getPrice() != null) {
            score += 2;
        }
        if (StringUtils.hasText(deal.getThumbnailUrl())) {
            score += 1;
        }
        return score;
    }

    private Comparator<DealGroupView> groupComparator(String sort) {
        if ("discount".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparing((DealGroupView group) -> group.representative().getDiscountRate() == null
                            ? Integer.MIN_VALUE
                            : group.representative().getDiscountRate())
                    .thenComparing(this::latestGroupTime)
                    .reversed();
        }
        return Comparator.comparing(this::latestGroupTime).reversed();
    }

    private OffsetDateTime latestGroupTime(DealGroupView group) {
        return group.members().stream()
                .map(this::dealSortTime)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.MIN);
    }

    private DealSummaryResponse toSummary(DealGroupView group) {
        OffsetDateTime latestPostedAt = group.members().stream()
                .map(Deal::getPostedAt)
                .max(Comparator.naturalOrder())
                .orElse(group.representative().getPostedAt());
        OffsetDateTime latestCollectedAt = group.members().stream()
                .map(Deal::getCollectedAt)
                .max(Comparator.naturalOrder())
                .orElse(group.representative().getCollectedAt());

        return DealSummaryResponse.from(
                group.representative(),
                group.members(),
                aggregateStatus(group.members()).name(),
                latestPostedAt,
                latestCollectedAt
        );
    }

    private DealStatus aggregateStatus(List<Deal> members) {
        if (members.stream().anyMatch(deal -> deal.getStatus() == DealStatus.ACTIVE)) {
            return DealStatus.ACTIVE;
        }
        if (members.stream().anyMatch(deal -> deal.getStatus() == DealStatus.SOLD_OUT)) {
            return DealStatus.SOLD_OUT;
        }
        return DealStatus.EXPIRED;
    }

    private OffsetDateTime dealSortTime(Deal deal) {
        if (deal.getPostedAt() != null) {
            return deal.getPostedAt();
        }
        if (deal.getCollectedAt() != null) {
            return deal.getCollectedAt();
        }
        return deal.getCreatedAt();
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (!StringUtils.hasText(currency)) {
            return DEFAULT_CURRENCY;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private record DealGroupView(Deal representative, List<Deal> members) {
    }
}
