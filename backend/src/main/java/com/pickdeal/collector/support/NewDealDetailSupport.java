package com.pickdeal.collector.support;

import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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
        List<CollectedDeal> enriched = new ArrayList<>(deals.size());
        int requested = 0;

        for (CollectedDeal deal : deals) {
            if (requested >= maxRequests) {
                enriched.add(deal);
                continue;
            }

            boolean isNew = !dealRepository.existsBySourceIdAndExternalId(source.getId(), deal.externalId());
            if (!isNew) {
                enriched.add(deal);
                continue;
            }

            requested++;
            try {
                CollectedDeal result = detailEnricher.apply(deal);
                enriched.add(result != null ? result : deal);
            } catch (RuntimeException e) {
                log.warn("딜 상세 수집 실패 [{}:{}] — 목록 정보만 저장", source.getCode(), deal.externalId(), e);
                enriched.add(deal);
            }
        }

        return List.copyOf(enriched);
    }
}
