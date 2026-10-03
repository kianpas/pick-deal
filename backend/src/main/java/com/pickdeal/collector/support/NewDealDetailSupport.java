package com.pickdeal.collector.support;

import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 신규 Deal에만 제한적으로 상세 정보를 보강하고, 실패는 목록 수집과 격리한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewDealDetailSupport {

    private final DealRepository dealRepository;

    public List<CollectedDeal> enrich(
            Source source,
            List<CollectedDeal> deals,
            int maxRequests,
            Function<CollectedDeal, CollectedDeal> detailEnricher
    ) {
        return enrich(source.getCode(), deals, maxRequests,
                deal -> !dealRepository.existsBySourceIdAndExternalId(source.getId(), deal.externalId()), detailEnricher);
    }

    /** 기존 ID 조회를 마친 수집기는 DB를 다시 조회하지 않고 상세를 요청한다. */
    public List<CollectedDeal> enrich(
            String sourceCode, Set<String> knownIds, List<CollectedDeal> deals, int maxRequests,
            Function<CollectedDeal, CollectedDeal> detailEnricher) {
        return enrich(sourceCode, deals, maxRequests, deal -> !knownIds.contains(deal.externalId()), detailEnricher);
    }

    private List<CollectedDeal> enrich(
            String sourceCode, List<CollectedDeal> deals, int maxRequests,
            Predicate<CollectedDeal> isNew, Function<CollectedDeal, CollectedDeal> detailEnricher) {
        List<CollectedDeal> enriched = new ArrayList<>(deals.size());
        int requested = 0;

        for (CollectedDeal deal : deals) {
            if (requested >= maxRequests) {
                enriched.add(deal);
                continue;
            }

            if (!isNew.test(deal)) {
                enriched.add(deal);
                continue;
            }

            requested++;
            try {
                CollectedDeal result = detailEnricher.apply(deal);
                enriched.add(result != null ? result : deal);
            } catch (RuntimeException e) {
                log.warn("딜 상세 수집 실패 [{}:{}] — 목록 정보만 저장", sourceCode, deal.externalId(), e);
                enriched.add(deal);
            }
        }

        return List.copyOf(enriched);
    }
}
