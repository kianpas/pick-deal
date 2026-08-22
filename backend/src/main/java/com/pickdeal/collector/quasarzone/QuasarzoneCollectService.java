package com.pickdeal.collector.quasarzone;

import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.DealUpsertSupport;
import com.pickdeal.collector.support.SourceCollector;
import com.pickdeal.source.domain.Source;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀘이사존 핫딜 수집: fetch(client) → parse(parser) → normalize(여기) → persist(support).
 */
@Service
@RequiredArgsConstructor
public class QuasarzoneCollectService implements SourceCollector {

    private static final String SOURCE_CODE = "quasarzone";
    private static final String SOURCE_NAME = "퀘이사존";
    private static final String SOURCE_BASE_URL = "https://quasarzone.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuasarzoneClient client;
    private final DealUpsertSupport upsertSupport;

    private final QuasarzoneListParser parser = new QuasarzoneListParser();
    private final QuasarzonePostedAtResolver postedAtResolver = new QuasarzonePostedAtResolver();

    @Override
    public String sourceCode() {
        return SOURCE_CODE;
    }

    @Override
    @Transactional
    public int collect() {
        OffsetDateTime now = OffsetDateTime.now(KST);
        Source source = upsertSupport.findOrRegisterSource(SOURCE_CODE, SOURCE_NAME, SOURCE_BASE_URL);

        List<CollectedDeal> deals = parser.parse(client.fetchListHtml()).stream()
                .map(item -> normalize(item, now))
                .toList();

        return upsertSupport.upsertAll(source, deals, now);
    }

    private CollectedDeal normalize(QuasarzoneDealItem item, OffsetDateTime now) {
        return new CollectedDeal(
                item.externalId(),
                item.url(),
                item.storeName(),
                item.title(),
                item.price(),
                item.category(),
                item.commentCount(),
                item.thumbnailUrl(),
                item.ended(),
                postedAtResolver.resolve(item.postedAtText(), now)
        );
    }
}
