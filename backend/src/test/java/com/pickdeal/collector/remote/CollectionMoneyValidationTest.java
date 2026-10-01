package com.pickdeal.collector.remote;

import static org.assertj.core.api.Assertions.assertThat;
import com.pickdeal.collector.dto.CollectionRequests;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CollectionMoneyValidationTest {
    @Test
    void allowsUsdCentsAndLegacyKrwButRejectsUnsupportedOrInvalidAmounts() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(item("382.76", "USD"))).isEmpty();
            assertThat(validator.validate(item("8550", null))).isEmpty();
            assertThat(validator.validate(item("0", "KRW"))).isEmpty();
            assertThat(validator.validate(item("382.76", null))).isNotEmpty();
            assertThat(validator.validate(item("382.76", "KRW"))).isNotEmpty();
            assertThat(validator.validate(item("382.765", "USD"))).isNotEmpty();
            assertThat(validator.validate(item("-1", "USD"))).isNotEmpty();
            assertThat(validator.validate(item("100", "EUR"))).isNotEmpty();
        }
    }

    private CollectionRequests.Item item(String amount, String currency) {
        return new CollectionRequests.Item("1989607", "https://quasarzone.com/bbs/qb_saleinfo/views/1989607",
                "알리", "미니PC", new BigDecimal(amount), "PC/하드웨어", 0,
                null, false, null, null, currency);
    }
}
