package com.pickdeal.deal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DealMatchNormalizerTest {

    @Test
    void removesOnlyVerifiedFreeShippingSuffixes() {
        String expected = DealMatchNormalizer.titleHash("방향제 100ml 1개", "네이버", 3900L);
        for (String suffix : new String[]{" (3,900원/무료)", " (3900원 / 무배)", " 3,900원 (무료배송)"}) {
            assertThat(DealMatchNormalizer.titleHash("[네이버] 방향제 100ml 1개" + suffix, "네이버", 3900L))
                    .isEqualTo(expected);
        }
    }

    @Test
    void preservesPricesWhenMissingMismatchedOrConditional() {
        String plain = "방향제 100ml 1개";
        for (String suffix : new String[]{" (카드 3,900원/무료)", " (3,900원/조건부 무료)",
                " (3,900원/N멤무배)", " (3,900원/3,000원)", " (정가 3,900원/무료)", " 3,900원"}) {
            assertThat(DealMatchNormalizer.titleHash(plain + suffix, "네이버", 3900L))
                    .isNotEqualTo(DealMatchNormalizer.titleHash(plain, "네이버", 3900L));
        }
        assertThat(DealMatchNormalizer.normalizeTitle(plain + " (3,900원/무료)", "네이버", 4000L)).endsWith("3900원무료");
        assertThat(DealMatchNormalizer.normalizeTitle(plain + " (3,900원/무료)", "네이버", null)).endsWith("3900원무료");
        assertThat(DealMatchNormalizer.normalizeTitle("상품 (99999999999999999999999원/무료)", null, 1000L))
                .contains("99999999999999999999999");
    }

    @Test
    void suffixRemovalPreservesModelCapacityQuantityAndPaymentConditions() {
        String base = DealMatchNormalizer.titleHash("SSD X1 1.5TB 1개", "네이버", 3900L);
        for (String title : new String[]{"SSD X2 1.5TB 1개", "SSD X1 15TB 1개", "SSD X1 1.5TB 2개",
                "SSD X1 1.5TB 1개 (네이버페이)"}) {
            assertThat(DealMatchNormalizer.titleHash(title + " (3,900원/무료)", "네이버", 3900L)).isNotEqualTo(base);
        }
    }

    @Test
    void mapsOnlyExplicitShopAliases() {
        assertThat(DealMatchNormalizer.normalizeShopName("G마켓")).isEqualTo("지마켓");
        assertThat(DealMatchNormalizer.normalizeShopName("네이버쇼핑")).isEqualTo("네이버");
        assertThat(DealMatchNormalizer.normalizeShopName("네이버페이")).isNotEqualTo("네이버");
        assertThat(DealMatchNormalizer.normalizeShopName("기타")).isNotEqualTo(DealMatchNormalizer.normalizeShopName("자사몰"));
    }

    @Test
    void preservesDecimalPointsBetweenDigits() {
        assertThat(DealMatchNormalizer.titleHash("우유 1.5L", null))
                .isNotEqualTo(DealMatchNormalizer.titleHash("우유 15L", null));
        assertThat(DealMatchNormalizer.titleHash("우유 １．５L", null))
                .isEqualTo(DealMatchNormalizer.titleHash("우유 1.5L", null));
    }

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
