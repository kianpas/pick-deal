package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryNormalizerTest {

    @Test
    @DisplayName("등록된 정확한 별칭은 PickDeal 대표 카테고리로 통일한다")
    void normalizesExactAlias() {
        assertThat(CategoryNormalizer.normalize("게임S/W")).isEqualTo("게임/SW");
        assertThat(CategoryNormalizer.normalize("  게임S/W  ")).isEqualTo("게임/SW");
    }

    @Test
    @DisplayName("일부 단어가 같아도 등록되지 않은 카테고리는 원문을 유지한다")
    void preservesUnregisteredCategory() {
        assertThat(CategoryNormalizer.normalize("게임H/W")).isEqualTo("게임H/W");
        assertThat(CategoryNormalizer.normalize("모바일게임")).isEqualTo("모바일게임");
        assertThat(CategoryNormalizer.normalize("상품권")).isEqualTo("상품권");
    }

    @Test
    @DisplayName("카테고리가 없으면 null을 유지한다")
    void preservesNull() {
        assertThat(CategoryNormalizer.normalize(null)).isNull();
    }
}
