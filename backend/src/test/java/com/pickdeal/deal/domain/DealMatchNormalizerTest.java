package com.pickdeal.deal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DealMatchNormalizerTest {

    @Test
    @DisplayName("출처가 확인한 판매몰 말머리와 공백·기호만 제거한다")
    void normalizesExactShopPrefixAndFormatting() {
        assertThat(DealMatchNormalizer.normalizeTitle("[쿠팡] 삼성 SSD 1TB", "쿠팡"))
                .isEqualTo(DealMatchNormalizer.normalizeTitle("삼성-SSD 1 TB", "쿠팡"));
    }

    @Test
    @DisplayName("판매몰이 아닌 말머리와 용량·수량은 보존한다")
    void preservesOptionTokens() {
        String oneTerabyte = DealMatchNormalizer.titleHash("[1TB] 삼성 SSD", "쿠팡");
        String twoTerabyte = DealMatchNormalizer.titleHash("[2TB] 삼성 SSD", "쿠팡");

        assertThat(oneTerabyte).isNotEqualTo(twoTerabyte);
    }

    @Test
    @DisplayName("유니코드 폭과 판매몰 표기의 사소한 차이를 통일한다")
    void normalizesUnicodeAndShopName() {
        assertThat(DealMatchNormalizer.normalizeShopName(" ＣＯＵＰＡＮＧ "))
                .isEqualTo("coupang");
    }
}
