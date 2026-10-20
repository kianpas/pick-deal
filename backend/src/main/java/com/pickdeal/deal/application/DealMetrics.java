package com.pickdeal.deal.application;

import com.pickdeal.deal.domain.DealRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 저장된 Deal 수 지표. 조회할 때마다 count를 실행한다. 보관 정리로 건수 상한이 있고
 * 지표는 내부 관리 포트에서만 조회하므로 비용이 작다. 목록 조회 성능 판단의 데이터 양 근거로 쓴다.
 */
@Component
public class DealMetrics {

    public DealMetrics(DealRepository dealRepository, MeterRegistry registry) {
        // Prometheus는 gauge 이름의 _total 접미사를 제거하므로 total 대신 stored를 쓴다.
        Gauge.builder("pickdeal.deals.stored", dealRepository, DealRepository::count)
                .description("저장된 Deal 수")
                .register(registry);
    }
}
