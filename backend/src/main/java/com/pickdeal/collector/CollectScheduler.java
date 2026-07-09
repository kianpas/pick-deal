package com.pickdeal.collector;

import com.pickdeal.collector.quasarzone.QuasarzoneCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수집 파이프라인 주기 실행. 실패해도 다음 주기에 다시 시도하므로 예외는 로그로만 남긴다.
 * 테스트/로컬에서 끄려면 {@code pickdeal.collector.scheduling.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickdeal.collector.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CollectScheduler {

    private final QuasarzoneCollectService quasarzoneCollectService;

    @Scheduled(initialDelayString = "PT15S", fixedDelayString = "PT10M")
    public void collect() {
        try {
            int saved = quasarzoneCollectService.collect();
            log.info("핫딜 수집 완료: 신규 {}건", saved);
        } catch (Exception e) {
            log.warn("핫딜 수집 실패 — 다음 주기에 재시도", e);
        }
    }
}
