package com.pickdeal.collector.ruliweb;

import com.pickdeal.collector.support.CategoryNormalizer;
import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.DealUpsertSupport;
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
 * 루리웹 핫딜 수집: fetch(client) → parse(parser) → normalize(여기) → persist(support).
 */
@Service
@ConditionalOnProperty(
        prefix = "pickdeal.collector.sources.ruliweb",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class RuliwebCollectService implements SourceCollector {

    private static final String SOURCE_CODE = "ruliweb";
    private static final String SOURCE_NAME = "루리웹";
    private static final String SOURCE_BASE_URL = "https://bbs.ruliweb.com";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final RuliwebClient client;
    private final DealUpsertSupport upsertSupport;
    private final RuliwebCollectorProperties properties;

    private final RuliwebListParser parser = new RuliwebListParser();
    private final RuliwebPostedAtResolver postedAtResolver = new RuliwebPostedAtResolver();

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

        return upsertSupport.upsertAll(source, deals, now);
    }

    private CollectedDeal normalize(RuliwebDealItem item, OffsetDateTime now) {
        return new CollectedDeal(
                item.externalId(),
                item.url(),
                item.storeName(),
                item.title(),
                item.price(),
                CategoryNormalizer.normalize(item.category()),
                item.commentCount(),
                null, // 루리웹 목록에는 썸네일이 없다
                item.ended(),
                postedAtResolver.resolve(item.postedAtText(), now)
        );
    }
}
