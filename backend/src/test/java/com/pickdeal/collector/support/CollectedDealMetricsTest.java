package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 저장 단계 지표는 실제 트랜잭션 경계에서 커밋된 결과만 센다. */
@SpringBootTest
class CollectedDealMetricsTest {

    @Autowired
    private DealUpsertSupport upsertSupport;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("커밋된 저장만 신규·갱신·제외 건수와 필드 확보율로 기록한다")
    void recordsCommittedDealsOnly() {
        var source = upsertSupport.findOrRegisterSource("metric-src", "지표 출처", "https://metric.example.com");
        var tx = new TransactionTemplate(transactionManager);
        var now = OffsetDateTime.now();

        tx.executeWithoutResult(status -> upsertSupport.upsertAll(source,
                List.of(deal("m-1", "쿠팡"), deal("m-2", null), deal("", null)), now));
        tx.executeWithoutResult(status -> upsertSupport.upsertAll(source, List.of(deal("m-1", "쿠팡")), now));
        tx.executeWithoutResult(status -> {
            upsertSupport.upsertAll(source, List.of(deal("rolled-back", "쿠팡"), deal("rolled-back-2", null)), now);
            status.setRollbackOnly();
        });

        assertThat(count("created")).isEqualTo(2);
        assertThat(count("updated")).isEqualTo(1);
        assertThat(count("skipped")).isEqualTo(1);
        // 마지막 커밋 배치(m-1 한 건, 판매처 있음) 기준. 롤백 배치(확보율 0.5)는 반영하지 않는다.
        assertThat(registry.get("pickdeal.collect.field.coverage").tags("source", "metric-src", "field", "shop")
                .gauge().value()).isEqualTo(1.0);
    }

    private double count(String result) {
        return registry.get("pickdeal.collect.deals").tags("source", "metric-src", "result", result).counter().count();
    }

    private static CollectedDeal deal(String externalId, String shop) {
        return new CollectedDeal(externalId, "https://metric.example.com/" + externalId, shop,
                "지표 테스트 " + externalId, 1_000L, null, null, null, false, OffsetDateTime.now());
    }
}
