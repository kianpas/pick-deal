package com.pickdeal.collector.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pickdeal.collector.dto.CollectionRequests;
import com.pickdeal.common.error.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RemoteCollectionMetricsTest {

    @Autowired
    private RemoteCollectionService service;

    @Autowired
    private MeterRegistry registry;

    @Test
    @DisplayName("원격 수신 배치를 수락·거부로 집계하고 지원하지 않는 출처 이름은 태그로 남기지 않는다")
    void recordsIngressOutcomes() {
        service.accept(batch("ppomppu", "https://www.ppomppu.co.kr/zboard/view.php?id=ppomppu&no=901"));
        assertThatThrownBy(() -> service.accept(batch("ppomppu", "https://evil.example.com/zboard/view.php?id=ppomppu&no=901")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.accept(batch("made-up-source", "https://example.com")))
                .isInstanceOf(BusinessException.class);

        assertThat(count("ppomppu", "accepted")).isEqualTo(1);
        assertThat(count("ppomppu", "rejected")).isEqualTo(1);
        assertThat(count("unknown", "rejected")).isEqualTo(1);
        assertThat(registry.find("pickdeal.ingress.batches").tag("source", "made-up-source").counter()).isNull();
    }

    private double count(String source, String outcome) {
        return registry.get("pickdeal.ingress.batches").tags("source", source, "outcome", outcome).counter().count();
    }

    private static CollectionRequests.Batch batch(String source, String url) {
        return new CollectionRequests.Batch(source, List.of(new CollectionRequests.Item(
                "901", url, "쿠팡", "지표 수신 테스트", new BigDecimal("1000"), null, null, null,
                false, OffsetDateTime.now(), null, "KRW")));
    }
}
