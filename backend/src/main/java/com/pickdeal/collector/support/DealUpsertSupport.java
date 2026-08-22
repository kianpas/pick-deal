package com.pickdeal.collector.support;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 출처와 무관한 저장 단계(persist)를 모은다. 중복은 (source, externalId) 유니크 제약과
 * 짝을 이루는 존재 조회로 차단하고, 기존 딜은 변동 가능한 값만 갱신한다.
 */
@Component
@RequiredArgsConstructor
public class DealUpsertSupport {

    private static final String DEFAULT_CURRENCY = "KRW";

    private final SourceRepository sourceRepository;
    private final DealRepository dealRepository;

    /** 출처가 없으면 등록하고 반환한다(첫 수집 시 자동 등록). */
    public Source findOrRegisterSource(String code, String name, String baseUrl) {
        return sourceRepository.findByCode(code)
                .orElseGet(() -> sourceRepository.save(new Source(name, baseUrl, code, true)));
    }

    /**
     * 수집 결과를 저장/갱신한다.
     *
     * @return 신규 저장된 딜 수
     */
    public int upsertAll(Source source, List<CollectedDeal> deals, OffsetDateTime now) {
        int saved = 0;
        for (CollectedDeal deal : deals) {
            if (upsert(source, deal, now)) {
                saved++;
            }
        }
        return saved;
    }

    private boolean upsert(Source source, CollectedDeal collected, OffsetDateTime now) {
        DealStatus status = collected.ended() ? DealStatus.EXPIRED : DealStatus.ACTIVE;
        return dealRepository.findBySourceIdAndExternalId(source.getId(), collected.externalId())
                .map(existing -> {
                    existing.updateFromRecollection(
                            collected.price(), collected.category(), collected.commentCount(), status);
                    return false;
                })
                .orElseGet(() -> {
                    dealRepository.save(toDeal(source, collected, status, now));
                    return true;
                });
    }

    private Deal toDeal(Source source, CollectedDeal collected, DealStatus status, OffsetDateTime now) {
        return new Deal(
                source,
                collected.rawTitle(),
                null,
                collected.price(),
                null,
                null,
                DEFAULT_CURRENCY,
                collected.category(),
                collected.commentCount(),
                collected.thumbnailUrl(),
                collected.url(),
                collected.externalId(),
                null,
                status,
                // 게시 시각을 해석하지 못했으면 수집 시각으로 둔다
                collected.postedAt() != null ? collected.postedAt() : now,
                now
        );
    }
}
