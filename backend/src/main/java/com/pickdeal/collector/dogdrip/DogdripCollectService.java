package com.pickdeal.collector.dogdrip;

import com.pickdeal.collector.support.DealUpsertSupport;
import com.pickdeal.collector.support.NewDealDetailSupport;
import com.pickdeal.collector.support.SourceCollector;
import java.time.OffsetDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Service
@ConditionalOnProperty(prefix = "pickdeal.collector.sources.dogdrip", name = "enabled", havingValue = "true")
public class DogdripCollectService implements SourceCollector {
    private final DogdripClient client;
    private final DealUpsertSupport upsertSupport;
    private final TransactionTemplate transactions;
    private final NewDealDetailSupport detailSupport;
    private final DogdripCollectorProperties properties;
    private final DogdripDetailParser detailParser = new DogdripDetailParser();
    private final DogdripListParser parser = new DogdripListParser();

    public DogdripCollectService(DogdripClient client, DealUpsertSupport upsertSupport,
                                PlatformTransactionManager transactionManager,
                                NewDealDetailSupport detailSupport, DogdripCollectorProperties properties) {
        this.client = client;
        this.upsertSupport = upsertSupport;
        this.transactions = new TransactionTemplate(transactionManager);
        this.detailSupport = detailSupport;
        this.properties = properties;
    }

    @Override public String sourceCode() { return "dogdrip"; }

    @Override public int collect() {
        var html = client.fetchListHtml();
        var now = OffsetDateTime.now();
        var deals = parser.parse(html, now);
        var source = transactions.execute(status ->
                upsertSupport.findOrRegisterSource(sourceCode(), "개드립", "https://www.dogdrip.net"));
        // HTTP 대기 중 DB 트랜잭션을 유지하지 않는다. 기존 글은 공통 보강기가 제외한다.
        var enriched = detailSupport.enrich(source, deals, properties.maxDetailRequests(), deal -> {
            var info = detailParser.parse(client.fetchDetailHtml(deal.url()));
            return deal.withProductInfo(info.shopName(), info.productUrl());
        });
        return transactions.execute(status -> upsertSupport.upsertAll(source, enriched, now));
    }
}
