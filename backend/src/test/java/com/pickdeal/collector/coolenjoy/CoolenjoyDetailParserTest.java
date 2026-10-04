package com.pickdeal.collector.coolenjoy;

import static org.assertj.core.api.Assertions.*;
import com.pickdeal.collector.support.CollectedDeal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CoolenjoyDetailParserTest {
    static String fixture() throws Exception {
        try (var stream = CoolenjoyDetailParserTest.class.getResourceAsStream("/fixtures/coolenjoy/jirum-detail.html")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    static CollectedDeal item() {
        return new CollectedDeal("3563135", "https://coolenjoy.net/bbs/jirum/3563135", "옥션", "잘린 제목…",
                36660, "의류잡화", 2, null, null, OffsetDateTime.parse("2026-10-04T00:00:00+09:00"), null);
    }
    @Test void extractsFullTitleBodyImageExactDateAndDisplayedProductLink() throws Exception {
        var deal = new CoolenjoyDetailParser().enrich(fixture(), item());
        assertThat(deal.title()).isEqualTo("아이더 신상 공용 플리스 자켓 36,660원 (무배)");
        assertThat(deal.storeName()).isEqualTo("옥션");
        assertThat(deal.thumbnailUrl()).startsWith("https://photo.coolenjoy.co.kr/data/editor/");
        assertThat(deal.productUrl()).isEqualTo("https://itempage3.auction.co.kr/DetailView.aspx?itemno=F667760519");
        assertThat(deal.postedAt()).isEqualTo("2026-10-04T11:15:12+09:00");
        assertThat(deal.price()).isEqualByComparingTo("36660");
        assertThat(deal.category()).isEqualTo("의류잡화");
    }
    @Test void rejectsWrongPostAndBlockResponses() throws Exception {
        String wrong = fixture().replace("property=\"og:url\" content=\"https://coolenjoy.net/bbs/jirum/3563135\"",
                "property=\"og:url\" content=\"https://coolenjoy.net/bbs/jirum/1\"");
        for (String html : new String[]{wrong, "<html>로그인이 필요합니다</html>", "<html>blocked</html>"})
            assertThatThrownBy(() -> new CoolenjoyDetailParser().enrich(html, item())).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void ignoresUnsafeTruncatedAndInternalLinksAndUnrelatedImages() throws Exception {
        for (String link : new String[]{"javascript:alert(1)", "https://shop.example/a…", "https://coolenjoy.net/bbs/jirum/1", "https://user:pass@shop.example/a"}) {
            String html = fixture().replace("https://itempage3.auction.co.kr/DetailView.aspx?itemno=F667760519", link);
            assertThat(new CoolenjoyDetailParser().enrich(html, item()).productUrl()).isNull();
        }
        var doc = org.jsoup.Jsoup.parse(fixture());
        doc.select(".view-content img").remove();
        doc.body().append("<img src='https://example.com/ad.jpg'><section id='bo_vc'><a href='https://example.com/comment'>댓글</a></section>");
        assertThat(new CoolenjoyDetailParser().enrich(doc.html(), item()).thumbnailUrl()).isNull();
    }
}
