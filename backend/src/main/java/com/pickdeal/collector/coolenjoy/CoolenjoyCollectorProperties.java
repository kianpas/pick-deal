package com.pickdeal.collector.coolenjoy;

import java.time.Duration;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("pickdeal.collector.sources.coolenjoy")
public record CoolenjoyCollectorProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10s") @DurationMin(seconds = 1) @DurationMax(seconds = 30) Duration timeout,
        @DefaultValue("3") @Min(0) @Max(3) int maxDetailRequests) {
}
