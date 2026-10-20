package com.pickdeal.collector.support;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 수집 운영 지표. 이름·태그를 한곳에서 관리하고, 태그 값은 출처 코드·결과 같은 소수의 고정값만 쓴다.
 * 저장 결과는 커밋 후에만 반영해 롤백된 배치를 수집량으로 세지 않는다.
 */
@Component
public class CollectorMetrics {

    private final MeterRegistry registry;
    private final Map<String, Instant> lastSuccess = new ConcurrentHashMap<>();
    private final Map<String, double[]> coverage = new ConcurrentHashMap<>();

    public CollectorMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** 앱 스케줄러의 출처별 수집 1회 결과와 소요 시간. */
    public void recordRun(String source, Duration elapsed, boolean success) {
        Timer.builder("pickdeal.collect.run")
                .description("출처별 수집 실행 시간과 결과")
                .tag("source", source)
                .tag("outcome", success ? "success" : "failure")
                .register(registry)
                .record(elapsed);
        if (success) {
            if (lastSuccess.put(source, Instant.now()) == null) {
                Gauge.builder("pickdeal.collect.last.success", lastSuccess,
                                values -> values.get(source).getEpochSecond())
                        .description("출처별 마지막 수집 성공 시각(epoch 초)")
                        .baseUnit("seconds")
                        .tag("source", source)
                        .register(registry);
            }
        }
    }

    /** 저장 단계 결과. 신규·갱신·검증 제외 건수를 커밋 후 반영한다. */
    public void recordDeals(String source, int created, int updated, int skipped) {
        afterCommit(() -> {
            count(source, "created", created);
            count(source, "updated", updated);
            count(source, "skipped", skipped);
        });
    }

    /** 마지막 저장 배치의 필드 확보율(0~1). 파서 선택자가 조용히 깨지는 것을 감시한다. */
    public void recordCoverage(String source, String field, double ratio) {
        afterCommit(() -> coverage.computeIfAbsent(source + ":" + field, key -> {
            double[] holder = new double[1];
            Gauge.builder("pickdeal.collect.field.coverage", holder, value -> value[0])
                    .description("마지막 저장 배치의 필드 확보율")
                    .tag("source", source)
                    .tag("field", field)
                    .register(registry);
            return holder;
        })[0] = ratio);
    }

    /** 원격 수신 배치 결과: accepted, conflict, rejected. */
    public void recordIngress(String source, String outcome) {
        registry.counter("pickdeal.ingress.batches", "source", source, "outcome", outcome).increment();
    }

    private void count(String source, String result, int amount) {
        if (amount > 0) {
            registry.counter("pickdeal.collect.deals", "source", source, "result", result).increment(amount);
        }
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
