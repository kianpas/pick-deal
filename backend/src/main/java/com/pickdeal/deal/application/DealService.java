package com.pickdeal.deal.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.common.response.PageMetaResponse;
import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealCategory;
import com.pickdeal.deal.dto.DealCategoryResponse;
import com.pickdeal.common.error.BusinessException;
import com.pickdeal.common.error.ErrorCode;
import com.pickdeal.deal.domain.DealListQueryRepository;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealMatchNormalizer;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.deal.domain.ShopFilterNames;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DealService {

    private static final Long DEFAULT_USER_ID = 1L;
    private static final String DEFAULT_CURRENCY = "KRW";
    private static final Pattern QUERY_WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private final DealRepository dealRepository;
    private final SourceRepository sourceRepository;
    private final KeywordRepository keywordRepository;
    private final DealGroupingService dealGroupingService;
    private final DealListQueryRepository dealListQueryRepository;

    /**
     * 최신순은 DB가 필터·그룹 접기·정렬·페이지 자르기를 처리하고, 이번 페이지 카드의 Deal만 읽어 응답을 만든다.
     * Deal 전체를 읽던 방식은 15,000건에서 p95 약 659ms였다(docs/notes C-3).
     * 할인율순은 대표 Deal 기준이라 기존 메모리 처리({@link #findDealsInMemory})를 유지한다. 화면에서는 쓰지 않는다.
     */
    @Transactional(readOnly = true)
    public DealListResponse findDeals(int page, int size, String sort, List<Long> sourceIds, String category, String query, List<String> shopNames, boolean hideEnded) {
        if ("discount".equalsIgnoreCase(sort)) {
            return findDealsInMemory(page, size, sort, sourceIds, category, query, shopNames, hideEnded);
        }
        DealCategory selectedCategory = parseCategory(category);
        List<String> queryTerms = queryTerms(query);
        List<String> excludeKeywords = lowerKeywords(KeywordType.EXCLUDE);
        List<Keyword> interestKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.INTEREST);

        // 별칭 규칙으로 선택 쇼핑몰에 해당하는 원본 이름을 구해 DB 조건으로 넘긴다.
        List<String> rawShopNames = null;
        if (shopNames != null && !shopNames.isEmpty()) {
            var selectedShops = shopNames.stream().map(ShopFilterNames::canonical)
                    .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            rawShopNames = dealRepository.countVisibleDealsByShopName(DEFAULT_USER_ID).stream()
                    .map(DealRepository.ShopCount::getShopName)
                    .filter(name -> selectedShops.contains(ShopFilterNames.canonical(name)))
                    .toList();
            if (rawShopNames.isEmpty()) return emptyPage(page, size);
        }
        // 분류 규칙으로 선택 카테고리에 해당하는 (출처, 원본 분류) 조합을 구해 DB 조건으로 넘긴다.
        List<DealListQueryRepository.SourceCategory> categories = null;
        if (selectedCategory != null) {
            categories = dealRepository.findVisibleSourceCategories(DEFAULT_USER_ID).stream()
                    .filter(row -> DealCategory.from(row.getSourceCode(), row.getCategory()) == selectedCategory)
                    .map(row -> new DealListQueryRepository.SourceCategory(row.getSourceCode(), row.getCategory()))
                    .toList();
            if (categories.isEmpty()) return emptyPage(page, size);
        }

        var criteria = new DealListQueryRepository.Criteria(DEFAULT_USER_ID,
                sourceIds == null || sourceIds.isEmpty() ? null : sourceIds,
                rawShopNames, categories, queryTerms, excludeKeywords,
                interestKeywords.stream().map(k -> k.getKeyword().toLowerCase(Locale.ROOT)).toList(), hideEnded);
        long total = dealListQueryRepository.countCards(criteria);
        List<Long> cardKeys = dealListQueryRepository.findCardKeys(criteria, (long) page * size, size);

        List<DealSummaryResponse> items = loadCards(cardKeys, sourceIds, rawShopNames).stream()
                .map(group -> toSummary(group, interestKeywords))
                .toList();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new DealListResponse(items, new PageMetaResponse(page, size, total, totalPages, (long) page + 1 < totalPages));
    }

    /** 카드 키 순서대로 구성원을 읽어 카드로 만든다. 구성원은 목록 조건을 만족하는 Deal만 포함한다. */
    private List<DealGroupView> loadCards(List<Long> cardKeys, List<Long> sourceIds, List<String> rawShopNames) {
        if (cardKeys.isEmpty()) return List.of();
        List<Long> groupIds = cardKeys.stream().filter(key -> key > 0).toList();
        List<Long> dealIds = cardKeys.stream().filter(key -> key < 0).map(key -> -key).toList();
        boolean allSources = sourceIds == null || sourceIds.isEmpty();
        boolean allShops = rawShopNames == null;
        Map<Long, List<Deal>> byCard = dealRepository.findVisibleCardMembers(DEFAULT_USER_ID,
                        groupIds.isEmpty() ? List.of(-1L) : groupIds,
                        dealIds.isEmpty() ? List.of(-1L) : dealIds,
                        allSources, allSources ? List.of(-1L) : sourceIds,
                        allShops, allShops ? List.of("") : rawShopNames).stream()
                .collect(Collectors.groupingBy(this::cardKey));
        return cardKeys.stream()
                .map(byCard::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toGroupView)
                .toList();
    }

    private DealListResponse emptyPage(int page, int size) {
        return new DealListResponse(List.of(), new PageMetaResponse(page, size, 0, 0, false));
    }

    private List<String> lowerKeywords(KeywordType type) {
        return keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, type).stream()
                .map(k -> k.getKeyword().toLowerCase(Locale.ROOT)).toList();
    }

    /**
     * 표시 대상 Deal 전체를 메모리로 읽어 필터·그룹·정렬·페이지 처리한다. 할인율순과, 최신순 DB 처리의 결과 비교 기준으로 쓴다.
     */
    @Transactional(readOnly = true)
    DealListResponse findDealsInMemory(int page, int size, String sort, List<Long> sourceIds, String category, String query, List<String> shopNames, boolean hideEnded) {
        DealCategory selectedCategory = parseCategory(category);
        List<String> queryTerms = queryTerms(query);
        var selectedShops = shopNames == null ? java.util.Set.<String>of() : shopNames.stream()
                .map(ShopFilterNames::canonical).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        List<Keyword> excludeKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.EXCLUDE);
        List<Keyword> interestKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.INTEREST);

        List<Deal> sourceEligibleDeals = dealRepository.findVisibleDeals(DEFAULT_USER_ID).stream()
                .filter(deal -> sourceIds == null || sourceIds.isEmpty() || sourceIds.contains(deal.getSource().getId()))
                .filter(deal -> shopNames == null || shopNames.isEmpty()
                        || (deal.getShopName() != null && selectedShops.contains(ShopFilterNames.canonical(deal.getShopName()))))
                .toList();

        List<DealGroupView> filteredGroups = groupDeals(sourceEligibleDeals).stream()
                .filter(group -> !hideEnded || aggregateStatus(group.members()) == DealStatus.ACTIVE)
                .filter(group -> group.members().stream().anyMatch(deal -> (selectedCategory == null || DealCategory.from(deal) == selectedCategory)))
                .filter(group -> group.members().stream().anyMatch(deal -> matchesQuery(deal, queryTerms)))
                .filter(group -> group.members().stream().noneMatch(deal -> containsAnyKeyword(deal, excludeKeywords)))
                .filter(group -> interestKeywords.isEmpty()
                        || group.members().stream().anyMatch(deal -> containsAnyKeyword(deal, interestKeywords)))
                .sorted(groupComparator(sort))
                .toList();

        int fromIndex = (int) Math.min((long) page * size, filteredGroups.size());
        int toIndex = (int) Math.min((long) fromIndex + size, filteredGroups.size());

        List<DealSummaryResponse> items = filteredGroups.subList(fromIndex, toIndex).stream()
                .map(group -> toSummary(group, interestKeywords))
                .toList();

        int totalPages = filteredGroups.isEmpty() ? 0 : (int) Math.ceil((double) filteredGroups.size() / size);
        boolean hasNext = (long) page + 1 < totalPages;

        return new DealListResponse(items, new PageMetaResponse(page, size, filteredGroups.size(), totalPages, hasNext));
    }

    /**
     * 표시 출처의 종료 딜도 포함하며, 분류 선언 순서로 반환한다.
     * DB는 (출처, 원본 분류) 조합만 돌려주고 표준 카테고리 변환은 {@link DealCategory}가 맡는다.
     * Deal 전체를 읽던 방식은 15,000건에서 p95 약 480ms였다(docs/notes C-3).
     */
    @Transactional(readOnly = true)
    public List<DealCategoryResponse> findCategories() {
        return dealRepository.findVisibleSourceCategories(DEFAULT_USER_ID).stream()
                .map(row -> DealCategory.from(row.getSourceCode(), row.getCategory()))
                .distinct().sorted()
                .map(DealCategoryResponse::from).toList();
    }

    /** 게시글 수가 많은 순, 같으면 이름순. DB가 원본 이름별 수를 세고 별칭은 여기서 합친다. */
    @Transactional(readOnly = true)
    public List<String> findShops() {
        return dealRepository.countVisibleDealsByShopName(DEFAULT_USER_ID).stream()
                .filter(row -> StringUtils.hasText(ShopFilterNames.canonical(row.getShopName())))
                .collect(Collectors.groupingBy(row -> ShopFilterNames.canonical(row.getShopName()),
                        Collectors.summingLong(DealRepository.ShopCount::getDealCount)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
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
                DealMatchNormalizer.titleHash(title, shopName, request.price()),
                DealStatus.ACTIVE,
                request.postedAt(),
                now
        ));
        dealGroupingService.groupIfMatched(deal);

        return DealDetailResponse.from(deal);
    }

    private DealCategory parseCategory(String category) {
        if (!StringUtils.hasText(category)) return null;
        try {
            return DealCategory.valueOf(category);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unknown category code: " + category);
        }
    }

    private List<String> queryTerms(String query) {
        if (!StringUtils.hasText(query)) return List.of();
        return Arrays.stream(QUERY_WHITESPACE.split(query.toLowerCase(Locale.ROOT).strip()))
                .filter(StringUtils::hasText).distinct().toList();
    }

    private boolean matchesQuery(Deal deal, List<String> terms) {
        // 모든 단어가 같은 원본 게시글의 제목 또는 본문에 있어야 한다.
        return terms.stream().allMatch(term -> contains(deal.getTitle(), term) || contains(deal.getDescription(), term));
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
        Map<Long, List<Deal>> grouped = new LinkedHashMap<>();
        for (Deal deal : deals) {
            grouped.computeIfAbsent(cardKey(deal), ignored -> new ArrayList<>()).add(deal);
        }

        return grouped.values().stream().map(this::toGroupView).toList();
    }

    /** 카드 식별자: 그룹이면 그룹 ID, 단독 Deal이면 음수 Deal ID. DB 처리와 같은 규칙이다. */
    private long cardKey(Deal deal) {
        return deal.getDealGroup() == null ? -deal.getId() : deal.getDealGroup().getId();
    }

    /** 구성원을 ID 순으로 고정해 대표 선택의 동률 처리가 조회 순서에 따라 달라지지 않게 한다. */
    private DealGroupView toGroupView(List<Deal> members) {
        List<Deal> sorted = members.stream().sorted(Comparator.comparing(Deal::getId)).toList();
        return new DealGroupView(selectRepresentative(sorted), sorted);
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

    /** 같은 값이면 카드 키 내림차순으로 고정한다(최신순 DB 처리와 같은 규칙). */
    private Comparator<DealGroupView> groupComparator(String sort) {
        Comparator<DealGroupView> byCardKey = Comparator.comparing(group -> cardKey(group.members().get(0)));
        if ("discount".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparing((DealGroupView group) -> group.representative().getDiscountRate() == null
                            ? Integer.MIN_VALUE
                            : group.representative().getDiscountRate())
                    .thenComparing(this::latestGroupTime)
                    .thenComparing(byCardKey)
                    .reversed();
        }
        return Comparator.comparing(this::latestGroupTime).thenComparing(byCardKey).reversed();
    }

    private OffsetDateTime latestGroupTime(DealGroupView group) {
        return group.members().stream()
                .map(this::dealSortTime)
                .max(Comparator.naturalOrder())
                .orElse(OffsetDateTime.MIN);
    }

    private DealSummaryResponse toSummary(DealGroupView group, List<Keyword> interestKeywords) {
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
                latestCollectedAt,
                matchedKeywords(group.members(), interestKeywords)
        );
    }

    /** 카드에 표시할 관심 키워드. 필터와 같은 포함 규칙으로 그룹 구성원 전체에서 찾는다. */
    private List<String> matchedKeywords(List<Deal> members, List<Keyword> interestKeywords) {
        return interestKeywords.stream()
                .map(Keyword::getKeyword)
                .filter(keyword -> members.stream()
                        .anyMatch(deal -> contains(deal.getTitle(), keyword) || contains(deal.getDescription(), keyword)))
                .distinct()
                .toList();
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
