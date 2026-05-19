package com.pickdeal.deal.application;

import com.pickdeal.common.error.DuplicateResourceException;
import com.pickdeal.common.error.ResourceNotFoundException;
import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.deal.dto.CreateDealRequest;
import com.pickdeal.deal.dto.DealDetailResponse;
import com.pickdeal.deal.dto.DealListResponse;
import com.pickdeal.deal.dto.DealSummaryResponse;
import com.pickdeal.preference.domain.KeywordType;
import com.pickdeal.preference.domain.PreferenceKeyword;
import com.pickdeal.preference.domain.PreferenceKeywordRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DealService {

    private final DealRepository dealRepository;
    private final SourceRepository sourceRepository;
    private final PreferenceKeywordRepository keywordRepository;

    public DealService(
            DealRepository dealRepository,
            SourceRepository sourceRepository,
            PreferenceKeywordRepository keywordRepository
    ) {
        this.dealRepository = dealRepository;
        this.sourceRepository = sourceRepository;
        this.keywordRepository = keywordRepository;
    }

    @Transactional(readOnly = true)
    public DealListResponse findDeals(int page, int size, Long sourceId, String query) {
        List<PreferenceKeyword> excludeKeywords = keywordRepository.findByTypeOrderByCreatedAtAsc(KeywordType.EXCLUDE);
        List<PreferenceKeyword> interestKeywords = keywordRepository.findByTypeOrderByCreatedAtAsc(KeywordType.INTEREST);

        List<Deal> filteredDeals = dealRepository.findVisibleDealsByStatus(DealStatus.ACTIVE).stream()
                .filter(deal -> sourceId == null || deal.getSource().getId().equals(sourceId))
                .filter(deal -> matchesQuery(deal, query))
                .filter(deal -> !containsAnyKeyword(deal, excludeKeywords))
                .sorted(Comparator.comparing(this::dealSortTime).reversed())
                .toList();

        int fromIndex = Math.min(page * size, filteredDeals.size());
        int toIndex = Math.min(fromIndex + size, filteredDeals.size());

        List<DealSummaryResponse> items = filteredDeals.subList(fromIndex, toIndex).stream()
                .map(deal -> DealSummaryResponse.from(deal, findMatchedKeywords(deal, interestKeywords)))
                .toList();

        int totalPages = filteredDeals.isEmpty() ? 0 : (int) Math.ceil((double) filteredDeals.size() / size);

        return new DealListResponse(items, page, size, filteredDeals.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public DealDetailResponse findDeal(Long dealId) {
        Deal deal = dealRepository.findByIdWithSource(dealId)
                .orElseThrow(() -> new ResourceNotFoundException("Deal not found: " + dealId));
        List<PreferenceKeyword> interestKeywords = keywordRepository.findByTypeOrderByCreatedAtAsc(KeywordType.INTEREST);

        return DealDetailResponse.from(deal, findMatchedKeywords(deal, interestKeywords));
    }

    @Transactional
    public DealDetailResponse createDeal(CreateDealRequest request) {
        Source source = sourceRepository.findById(request.sourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Source not found: " + request.sourceId()));

        String originalUrl = request.originalUrl().trim();
        if (dealRepository.existsBySourceIdAndOriginalUrl(source.getId(), originalUrl)) {
            throw new DuplicateResourceException("Deal already exists for source and originalUrl");
        }

        Deal deal = dealRepository.save(new Deal(
                source,
                request.title().trim(),
                normalizeNullable(request.description()),
                request.price(),
                request.shippingFee(),
                originalUrl,
                normalizeNullable(request.originalId()),
                DealStatus.ACTIVE,
                request.postedAt(),
                OffsetDateTime.now()
        ));

        List<PreferenceKeyword> interestKeywords = keywordRepository.findByTypeOrderByCreatedAtAsc(KeywordType.INTEREST);
        return DealDetailResponse.from(deal, findMatchedKeywords(deal, interestKeywords));
    }

    private boolean matchesQuery(Deal deal, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }

        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        return contains(deal.getTitle(), normalizedQuery) || contains(deal.getDescription(), normalizedQuery);
    }

    private boolean containsAnyKeyword(Deal deal, List<PreferenceKeyword> keywords) {
        return keywords.stream()
                .map(PreferenceKeyword::getValue)
                .anyMatch(keyword -> contains(deal.getTitle(), keyword) || contains(deal.getDescription(), keyword));
    }

    private List<String> findMatchedKeywords(Deal deal, List<PreferenceKeyword> keywords) {
        return keywords.stream()
                .map(PreferenceKeyword::getValue)
                .filter(keyword -> contains(deal.getTitle(), keyword) || contains(deal.getDescription(), keyword))
                .toList();
    }

    private boolean contains(String text, String keyword) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
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
}

