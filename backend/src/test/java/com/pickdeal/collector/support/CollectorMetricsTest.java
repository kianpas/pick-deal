package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.collector.CollectScheduler;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectorMetricsTest {

    @Test
    @DisplayName("스케줄러는 출처별 수집 결과·시간을 기록하고 성공한 출처만 마지막 성공 시각을 남긴다")
    void schedulerRecordsRunOutcomes() {
        MeterRegistry registry = new SimpleMeterRegistry();
        SourceCollector ok = collector("ok-source", false);
        SourceCollector broken = collector("broken-source", true);
        CollectScheduler scheduler = new CollectScheduler(List.of(ok, broken), new CollectorMetrics(registry));

        scheduler.collect();
        scheduler.collect();

        assertThat(registry.get("pickdeal.collect.run").tags("source", "ok-source", "outcome", "success").timer().count())
                .isEqualTo(2);
        assertThat(registry.get("pickdeal.collect.run").tags("source", "broken-source", "outcome", "failure").timer().count())
                .isEqualTo(2);
        assertThat(registry.get("pickdeal.collect.last.success").tag("source", "ok-source").gauge().value()).isPositive();
        assertThat(registry.find("pickdeal.collect.last.success").tag("source", "broken-source").gauge()).isNull();
    }

    private static SourceCollector collector(String code, boolean fails) {
        return new SourceCollector() {
            @Override
            public String sourceCode() {
                return code;
            }

            @Override
            public int collect() {
                if (fails) throw new IllegalStateException("수집 실패");
                return 1;
            }
        };
    }
}
