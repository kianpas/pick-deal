package com.pickdeal.deal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DealCategoryTest {
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
