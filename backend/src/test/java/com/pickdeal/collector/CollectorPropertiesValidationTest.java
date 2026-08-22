package com.pickdeal.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.collector.quasarzone.QuasarzoneCollectorProperties;
import com.pickdeal.collector.ruliweb.RuliwebCollectorProperties;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectorPropertiesValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("페이지와 항목 한도는 1 이상이어야 한다")
    void rejectsNonPositiveLimits() {
        QuasarzoneCollectorProperties properties =
                new QuasarzoneCollectorProperties(true, Duration.ofSeconds(10), 0, 0, 0, 0);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "maxPages", "maxItems", "bootstrapMaxPages", "bootstrapMaxItems");
    }

    @Test
    @DisplayName("HTTP timeout은 Jsoup이 처리할 수 있는 양의 범위여야 한다")
    void rejectsInvalidTimeout() {
        RuliwebCollectorProperties zero =
                new RuliwebCollectorProperties(true, Duration.ZERO, 1, 1, 3, 150);
        RuliwebCollectorProperties tooLarge =
                new RuliwebCollectorProperties(
                        true, Duration.ofMillis((long) Integer.MAX_VALUE + 1), 1, 1, 3, 150);

        assertThat(validator.validate(zero))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("timeout");
        assertThat(validator.validate(tooLarge))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("timeout");
    }
}
