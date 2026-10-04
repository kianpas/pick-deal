package com.pickdeal.deal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DealCategoryTest {
    @Test void coolenjoyCategoriesAreMappedWithoutChangingOtherSources() {
        var mappings = java.util.Map.ofEntries(
                java.util.Map.entry("PC관련", DealCategory.PC), java.util.Map.entry("가전", DealCategory.DIGITAL),
                java.util.Map.entry("모바일", DealCategory.DIGITAL), java.util.Map.entry("게임", DealCategory.GAME),
                java.util.Map.entry("식품", DealCategory.FOOD), java.util.Map.entry("의류잡화", DealCategory.FASHION),
                java.util.Map.entry("화장품", DealCategory.FASHION), java.util.Map.entry("쿠폰", DealCategory.BENEFIT),
                java.util.Map.entry("이벤트", DealCategory.BENEFIT), java.util.Map.entry("인터넷", DealCategory.ETC));
        mappings.forEach((raw, expected) -> assertThat(DealCategory.from("coolenjoy", raw)).isEqualTo(expected));
        assertThat(DealCategory.from("other", "PC관련")).isEqualTo(DealCategory.ETC);
    }
    @Test void exactAliasesAndSourceOverrides() {
        assertThat(DealCategory.from("quasarzone", "생활/식품")).isEqualTo(DealCategory.FOOD);
        assertThat(DealCategory.from("unknown", "생활/식품")).isEqualTo(DealCategory.ETC);
        assertThat(DealCategory.from("dogdrip", "생활용품")).isEqualTo(DealCategory.LIVING);
        assertThat(DealCategory.from("ruliweb", "게임S/W")).isEqualTo(DealCategory.GAME);
        assertThat(DealCategory.from("ruliweb", "게임H/W")).isEqualTo(DealCategory.GAME);
        assertThat(DealCategory.from(null, " PC/하드웨어 ")).isEqualTo(DealCategory.PC);
        assertThat(DealCategory.from("dogdrip", "포인트/래플")).isEqualTo(DealCategory.BENEFIT);
    }

    @Test void unrecognizedAndMissingValuesAreOtherWithoutGuessing() {
        assertThat(DealCategory.from(null, null)).isEqualTo(DealCategory.ETC);
        assertThat(DealCategory.from("dogdrip", " ")).isEqualTo(DealCategory.ETC);
        assertThat(DealCategory.from("quasarzone", "식품 특별전")).isEqualTo(DealCategory.ETC);
    }
}
