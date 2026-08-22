package com.pickdeal.collector.ruliweb;

import jakarta.validation.constraints.Min;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/** 루리웹 수집기의 요청 제한과 실행당 처리 상한. */
@Validated
@ConfigurationProperties(prefix = "pickdeal.collector.sources.ruliweb")
public record RuliwebCollectorProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("10s") @DurationMin(millis = 1) @DurationMax(millis = Integer.MAX_VALUE) Duration timeout,
        @DefaultValue("1") @Min(1) int maxPages,
        @DefaultValue("50") @Min(1) int maxItems
) {
}
