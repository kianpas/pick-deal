package com.pickdeal.collector.quasarzone;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 핫딜 목록 수집 파이프라인: fetch(client) → parse(parser) → normalize/persist(여기).
 * 중복은 (source, externalId) 유니크 제약 + 존재 조회로 차단하고, 기존 딜은 가격/상태만 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class QuasarzoneCollectService {

    private static final String SOURCE_CODE = "quasarzone";
    private static final String SOURCE_NAME = "퀘이사존";
    private static final String SOURCE_BASE_URL = "https://quasarzone.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuasarzoneClient client;
    private final SourceRepository sourceRepository;
    private final DealRepository dealRepository;

    private final QuasarzoneListParser parser = new QuasarzoneListParser();
    private final QuasarzonePostedAtResolver postedAtResolver = new QuasarzonePostedAtResolver();

    /**
     * 목록 1페이지를 수집해 신규 딜을 저장하고 기존 딜을 갱신한다.
     *
     * @return 신규 저장된 딜 수
     */
    @Transactional
    public int collect() {
        OffsetDateTime now = OffsetDateTime.now(KST);
        Source source = findOrRegisterSource();
        List<QuasarzoneDealItem> items = parser.parse(client.fetchListHtml());

        int saved = 0;
        for (QuasarzoneDealItem item : items) {
            boolean inserted = upsert(source, item, now);
            if (inserted) {
                saved++;
            }
        }
        return saved;
    }

    private Source findOrRegisterSource() {
        return sourceRepository.findByCode(SOURCE_CODE)
                .orElseGet(() -> sourceRepository.save(
                        new Source(SOURCE_NAME, SOURCE_BASE_URL, SOURCE_CODE, true)));
    }

    private boolean upsert(Source source, QuasarzoneDealItem item, OffsetDateTime now) {
        DealStatus status = item.ended() ? DealStatus.EXPIRED : DealStatus.ACTIVE;
        return dealRepository.findBySourceIdAndExternalId(source.getId(), item.externalId())
                .map(existing -> {
                    existing.updateFromRecollection(item.price(), status);
                    return false;
                })
                .orElseGet(() -> {
                    dealRepository.save(toDeal(source, item, status, now));
                    return true;
                });
    }

    private Deal toDeal(Source source, QuasarzoneDealItem item, DealStatus status, OffsetDateTime now) {
        // 게시 시각은 목록의 상대 표기를 근사 변환하고, 해석 불가하면 수집 시각으로 둔다
        OffsetDateTime postedAt = postedAtResolver.resolve(item.postedAtText(), now);
        // 제목은 출처에 게시된 원문 그대로("[판매처] ..." 접두사 포함) 저장한다
        String rawTitle = item.storeName() != null
                ? "[" + item.storeName() + "] " + item.title()
                : item.title();
        return new Deal(
                source,
                rawTitle,
                null,
                item.price(),
                null,
                null,
                "KRW",
                item.category(),
                item.thumbnailUrl(),
                item.url(),
                item.externalId(),
                null,
                status,
                postedAt != null ? postedAt : now,
                now
        );
    }
}
