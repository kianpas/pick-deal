package com.pickdeal.deal.application;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 보관 기간 정리 주기 실행. 재시작해도 주기가 밀리지 않도록 fixed-delay가 아니라 cron 시각에 실행한다.
 * 기본 OFF이며 {@code pickdeal.retention.enabled=true}일 때만 등록된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pickdeal.retention.enabled", havingValue = "true")
public class DealRetentionScheduler {

    private final DealRetentionService retentionService;
    private final DealRetentionProperties properties;

    @Scheduled(cron = "${pickdeal.retention.cron:0 0 4 * * MON}", zone = "Asia/Seoul")
    public void purge() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(properties.days());
        try {
            DealRetentionService.Result result = retentionService.purgeOlderThan(cutoff);
            log.info("보관 기간 정리 완료: {}일 이전 Deal {}건, 그룹 {}건 삭제",
                    properties.days(), result.deletedDeals(), result.deletedGroups());
        } catch (Exception e) {
            log.warn("보관 기간 정리 실패 — 다음 주기에 재시도", e);
        }
    }
}
