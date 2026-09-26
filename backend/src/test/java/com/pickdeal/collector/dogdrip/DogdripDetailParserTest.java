package com.pickdeal.collector.dogdrip;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DogdripDetailParserTest {
    private final DogdripDetailParser parser = new DogdripDetailParser();

    static String fixture() throws Exception {
        try (var input = DogdripDetailParserTest.class.getResourceAsStream("/fixtures/dogdrip/hotdeal-detail.html")) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test void actualLinkWithAffiliateIcon() throws Exception {
        assertThat(parser.parse(fixture()).productUrl()).isEqualTo(
                "https://sharkninja.co.kr/board/event/read.html?no=1610&board_no=8&category_no=1&cate_no=1&category_no=1");
    }

    @Test void onlyProductTableLinksAreAccepted() {
        for (String url : new String[]{"javascript:openAd()", "/link.php?url=anything",
                "https://www.dogdrip.net/link.php", "https://user:pass@shop.example.com/item"}) {
            assertThat(parser.parse("<table class='ed extra-value'><tr><th>링크</th><td><a href='" + url
                    + "'>상품</a></td></tr></table><a href='https://ads.example.com'>광고</a>").productUrl()).isNull();
        }
        assertThat(parser.parse("<div class='xe_content'><a href='https://shop.example.com'>댓글</a></div>")
                .productUrl()).isNull();
        assertThat(parser.parse("<table class='ed extra-value'><tr><th>참고 링크</th><td><a href='https://shop.example.com'>상품</a></td></tr></table>")
                .productUrl()).isNull();
        assertThatThrownBy(() -> parser.parse("challenge-platform")).isInstanceOf(IllegalArgumentException.class);
    }
}
