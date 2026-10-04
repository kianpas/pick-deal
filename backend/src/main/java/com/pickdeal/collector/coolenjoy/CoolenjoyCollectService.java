package com.pickdeal.collector.coolenjoy;

import com.pickdeal.collector.support.DealUpsertSupport;
import com.pickdeal.collector.support.NewDealDetailSupport;
import com.pickdeal.collector.support.SourceCollector;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pickdeal.collector.sources.coolenjoy", name = "enabled", havingValue = "true")
public class CoolenjoyCollectService implements SourceCollector {
    private final CoolenjoyClient client;
    private final DealUpsertSupport upsertSupport;
    private final NewDealDetailSupport detailSupport;
    private final CoolenjoyCollectorProperties properties;
    private final CoolenjoyListParser parser = new CoolenjoyListParser();
    private final CoolenjoyDetailParser detailParser = new CoolenjoyDetailParser();

    @Override public String sourceCode() { return "coolenjoy"; }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int collect() {
        var deals = parser.parse(client.fetchListHtml(), OffsetDateTime.now());
        if (deals.isEmpty()) return 0;
        var knownIds = properties.maxDetailRequests() > 0
                ? upsertSupport.knownExternalIds(sourceCode(), deals) : java.util.Set.<String>of();
        deals = detailSupport.enrich(sourceCode(), knownIds, deals, properties.maxDetailRequests(),
                deal -> detailParser.enrich(client.fetchDetailHtml(deal.url()), deal));
        return upsertSupport.upsertAll(sourceCode(), "쿨엔조이", "https://coolenjoy.net", deals, OffsetDateTime.now());
    }
}
