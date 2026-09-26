package com.pickdeal.collector.dogdrip;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import org.hibernate.validator.constraints.time.DurationMin;
import org.hibernate.validator.constraints.time.DurationMax;

@Validated
@ConfigurationProperties("pickdeal.collector.sources.dogdrip")
public record DogdripCollectorProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("10s") @DurationMin(seconds = 1) @DurationMax(seconds = 30) Duration timeout,
        @DefaultValue("3") @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(3) int maxDetailRequests) {
}
