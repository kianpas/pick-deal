package com.pickdeal.deal.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.common.response.PageMetaResponse;
import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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

    // MVP 한정: ACTIVE 딜 전체를 메모리로 올린 뒤 필터/정렬/페이지네이션한다.
    // 키워드 필터가 제목·본문 부분일치라 DB로 내리기 애매한 점을 감안한 소규모 시드 전용 구현.
    // 데이터가 커지면 DB 쿼리/키셋 페이지네이션으로 전환한다(docs/03 §5).
    @Transactional(readOnly = true)
    public DealListResponse findDeals(int page, int size, String sort, List<Long> sourceIds, String category, String query) {
        List<Keyword> excludeKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.EXCLUDE);
        List<Keyword> interestKeywords = keywordRepository.findByUserIdAndTypeOrderByCreatedAtAsc(DEFAULT_USER_ID, KeywordType.INTEREST);

        List<Deal> filteredDeals = dealRepository.findVisibleDealsByStatus(DealStatus.ACTIVE, DEFAULT_USER_ID).stream()
                .filter(deal -> sourceIds == null || sourceIds.isEmpty() || sourceIds.contains(deal.getSource().getId()))
                .filter(deal -> matchesCategory(deal, category))
                .filter(deal -> matchesQuery(deal, query))
                .filter(deal -> !containsAnyKeyword(deal, excludeKeywords))
                .filter(deal -> interestKeywords.isEmpty() || containsAnyKeyword(deal, interestKeywords))
                .sorted(dealComparator(sort))
                .toList();

        int fromIndex = Math.min(page * size, filteredDeals.size());
        int toIndex = Math.min(fromIndex + size, filteredDeals.size());

        List<DealSummaryResponse> items = filteredDeals.subList(fromIndex, toIndex).stream()
                .map(DealSummaryResponse::from)
                .toList();

        int totalPages = filteredDeals.isEmpty() ? 0 : (int) Math.ceil((double) filteredDeals.size() / size);
        boolean hasNext = page + 1 < totalPages;

        return new DealListResponse(items, new PageMetaResponse(page, size, filteredDeals.size(), totalPages, hasNext));
    }

    @Transactional(readOnly = true)
    public DealDetailResponse findDeal(Long dealId) {
        Deal deal = dealRepository.findByIdWithSource(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));

        return DealDetailResponse.from(deal);
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
        Deal deal = dealRepository.save(new Deal(
                source,
                request.title().trim(),
                normalizeNullable(request.description()),
                request.price(),
                request.originalPrice(),
                request.discountRate(),
                normalizeCurrency(request.currency()),
                normalizeNullable(request.category()),
                normalizeNullable(request.thumbnailUrl()),
                request.originalUrl().trim(),
                externalId,
                null,
                DealStatus.ACTIVE,
                request.postedAt(),
                now
        ));

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

    private Comparator<Deal> dealComparator(String sort) {
        if ("discount".equalsIgnoreCase(sort)) {
            return Comparator
                    .comparing((Deal deal) -> deal.getDiscountRate() == null ? Integer.MIN_VALUE : deal.getDiscountRate())
                    .thenComparing(this::dealSortTime)
                    .reversed();
        }
        return Comparator.comparing(this::dealSortTime).reversed();
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
}
