package com.pickdeal.collector.quasarzone;

import com.pickdeal.collector.support.CategoryNormalizer;
import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.CollectedProductInfo;
import com.pickdeal.collector.support.DealUpsertSupport;
import com.pickdeal.collector.support.NewDealDetailSupport;
import com.pickdeal.collector.support.PagedCollectionSupport;
import com.pickdeal.collector.support.SourceCollector;
import com.pickdeal.source.domain.Source;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀘이사존 핫딜 수집: fetch(client) → parse(parser) → normalize(여기) → persist(support).
 */
@Service
@ConditionalOnProperty(
        prefix = "pickdeal.collector.sources.quasarzone",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class QuasarzoneCollectService implements SourceCollector {

    private static final String SOURCE_CODE = "quasarzone";
    private static final String SOURCE_NAME = "퀘이사존";
    private static final String SOURCE_BASE_URL = "https://quasarzone.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final QuasarzoneClient client;
    private final DealUpsertSupport upsertSupport;
    private final NewDealDetailSupport detailSupport;
    private final QuasarzoneCollectorProperties properties;

    private final QuasarzoneListParser parser = new QuasarzoneListParser();
    private final QuasarzoneDetailParser detailParser = new QuasarzoneDetailParser();
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
        boolean bootstrap = !upsertSupport.hasCollectedDeals(source);
        int maxPages = bootstrap ? properties.bootstrapMaxPages() : properties.maxPages();
        int maxItems = bootstrap ? properties.bootstrapMaxItems() : properties.maxItems();
        List<CollectedDeal> deals = PagedCollectionSupport.collect(
                maxPages,
                maxItems,
                client::fetchListHtml,
                parser::parse,
                item -> normalize(item, now)
        );
        deals = detailSupport.enrich(
                source, deals, properties.maxDetailRequests(), this::enrichProductInfo);

        return upsertSupport.upsertAll(source, deals, now);
    }

    private CollectedDeal enrichProductInfo(CollectedDeal deal) {
        CollectedProductInfo productInfo = detailParser.parse(client.fetchDetailHtml(deal.url()));
        return deal.withProductInfo(productInfo.shopName(), productInfo.productUrl());
    }

    private CollectedDeal normalize(QuasarzoneDealItem item, OffsetDateTime now) {
        return new CollectedDeal(
                item.externalId(),
                item.url(),
                item.storeName(),
                item.title(),
                item.price(),
                CategoryNormalizer.normalize(item.category()),
                item.commentCount(),
                item.thumbnailUrl(),
                item.ended(),
                postedAtResolver.resolve(item.postedAtText(), now)
        );
    }
}
