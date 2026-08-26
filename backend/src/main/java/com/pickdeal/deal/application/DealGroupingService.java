package com.pickdeal.deal.application;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealGroup;
import com.pickdeal.deal.domain.DealGroupRepository;
import com.pickdeal.deal.domain.DealMatchNormalizer;
import com.pickdeal.deal.domain.DealRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 신규·재수집 Deal을 서로 다른 출처의 강한 일치 후보와 보수적으로 그룹화한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DealGroupingService {

    private final DealRepository dealRepository;
    private final DealGroupRepository dealGroupRepository;

    @Transactional
    public void groupIfMatched(Deal deal) {
        if (deal.getDealGroup() != null) {
            deal.getDealGroup().considerRepresentative(deal);
            return;
        }
        if (deal.getSource().getId() == null) {
            return;
        }

        List<Deal> matches = findCandidates(deal).stream()
                .filter(candidate -> isStrongMatch(deal, candidate))
                .toList();
        if (matches.isEmpty()) {
            return;
        }

        Set<Long> matchedSourceIds = new HashSet<>();
        matchedSourceIds.add(deal.getSource().getId());
        for (Deal match : matches) {
            if (!matchedSourceIds.add(match.getSource().getId())) {
                log.warn("같은 출처의 후보가 여러 건이라 자동 그룹화를 건너뜀 [dealId={}]", deal.getId());
                return;
            }
        }

        Map<Long, DealGroup> existingGroups = new LinkedHashMap<>();
        for (Deal match : matches) {
            DealGroup group = match.getDealGroup();
            if (group != null) {
                existingGroups.put(group.getId(), group);
            }
        }
        if (existingGroups.size() > 1) {
            log.warn("교차 출처 그룹 후보가 여러 기존 그룹과 충돌해 자동 그룹화를 건너뜀 [dealId={}]", deal.getId());
            return;
        }

        DealGroup group = existingGroups.values().stream()
                .findFirst()
                .orElseGet(() -> dealGroupRepository.save(new DealGroup(matches.get(0))));

        for (Deal match : matches) {
            if (match.getDealGroup() == null) {
                match.joinGroup(group);
            }
            group.considerRepresentative(match);
        }
        deal.joinGroup(group);
        group.considerRepresentative(deal);
    }

    /**
     * 후보 탐색(retrieval)과 판정(scoring)을 분리한다. 탐색은 강한 근거를 각각 독립적으로 훑고,
     * 합칠지 여부는 {@link #isStrongMatch}가 판단한다. 제목 해시만 검색 키로 쓰면 상품 URL이
     * 같아도 제목 표현이 다른 실제 중복을 놓친다.
     */
    private List<Deal> findCandidates(Deal deal) {
        Long sourceId = deal.getSource().getId();
        Map<Long, Deal> candidates = new LinkedHashMap<>();

        if (hasText(deal.getProductUrl())) {
            dealRepository.findCrossSourceCandidatesByProductUrl(sourceId, deal.getProductUrl().trim())
                    .forEach(candidate -> candidates.put(candidate.getId(), candidate));
        }
        if (deal.getTitleNormHash() != null) {
            dealRepository.findCrossSourceCandidates(sourceId, deal.getTitleNormHash())
                    .forEach(candidate -> candidates.put(candidate.getId(), candidate));
        }
        return List.copyOf(candidates.values());
    }

    private boolean isStrongMatch(Deal deal, Deal candidate) {
        boolean sameProductUrl = hasText(deal.getProductUrl())
                && hasText(candidate.getProductUrl())
                && Objects.equals(deal.getProductUrl().trim(), candidate.getProductUrl().trim());
        if (sameProductUrl) {
            return true;
        }

        String shopKey = DealMatchNormalizer.normalizeShopName(deal.getShopName());
        String candidateShopKey = DealMatchNormalizer.normalizeShopName(candidate.getShopName());
        return shopKey != null
                && shopKey.equals(candidateShopKey)
                && deal.getPrice() != null
                && deal.getPrice().equals(candidate.getPrice());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
