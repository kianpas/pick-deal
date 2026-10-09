package com.pickdeal.deal.application;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 오래된 Deal 정리 설정. 실행 주기는 {@code pickdeal.retention.cron}으로 {@link DealRetentionScheduler}가 읽는다. */
@Validated
@ConfigurationProperties("pickdeal.retention")
public record DealRetentionProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("90") @Min(30) int days) {
}
