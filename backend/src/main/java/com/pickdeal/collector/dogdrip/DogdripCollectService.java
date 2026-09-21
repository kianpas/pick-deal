package com.pickdeal.collector.dogdrip;

import com.pickdeal.collector.support.DealUpsertSupport;
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
    private final DogdripListParser parser = new DogdripListParser();

    public DogdripCollectService(DogdripClient client, DealUpsertSupport upsertSupport,
                                PlatformTransactionManager transactionManager) {
        this.client = client;
        this.upsertSupport = upsertSupport;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override public String sourceCode() { return "dogdrip"; }

    @Override public int collect() {
        var html = client.fetchListHtml();
        var now = OffsetDateTime.now();
        var deals = parser.parse(html, now);
        return transactions.execute(status -> {
            var source = upsertSupport.findOrRegisterSource(sourceCode(), "개드립", "https://www.dogdrip.net");
            return upsertSupport.upsertAll(source, deals, now);
        });
    }
}
