package com.pickdeal.collector;

import com.pickdeal.collector.support.SourceCollector;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수집 파이프라인 주기 실행. 등록된 모든 출처를 순회하므로 출처가 늘어도 여기는 바뀌지 않는다.
 * 한 출처가 실패해도 나머지는 계속 수집하고, 실패는 로그로만 남긴다(다음 주기에 재시도).
 * 테스트/로컬에서 끄려면 {@code pickdeal.collector.scheduling.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickdeal.collector.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CollectScheduler {

    private final List<SourceCollector> collectors;

    @Scheduled(initialDelayString = "PT15S", fixedDelayString = "PT20M")
    public void collect() {
        for (SourceCollector collector : collectors) {
            try {
                int saved = collector.collect();
                log.info("핫딜 수집 완료 [{}]: 신규 {}건", collector.sourceCode(), saved);
            } catch (Exception e) {
                log.warn("핫딜 수집 실패 [{}] — 다음 주기에 재시도", collector.sourceCode(), e);
            }
        }
    }
}
