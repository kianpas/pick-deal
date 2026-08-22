package com.pickdeal.collector.ruliweb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pickdeal.collector.support.HtmlFetcher;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RuliwebClientTest {

    @Test
    @DisplayName("첫 페이지 URL과 출처별 timeout을 HTML 요청에 적용한다")
    void fetchesConfiguredPageWithTimeout() {
        HtmlFetcher fetcher = Mockito.mock(HtmlFetcher.class);
        Duration timeout = Duration.ofSeconds(12);
        RuliwebCollectorProperties properties =
                new RuliwebCollectorProperties(true, timeout, 3, 30);
        RuliwebClient client = new RuliwebClient(fetcher, properties);
        given(fetcher.fetch("https://bbs.ruliweb.com/market/board/1020", timeout))
                .willReturn("<html>page 1</html>");

        String html = client.fetchListHtml(1);

        assertThat(html).isEqualTo("<html>page 1</html>");
        verify(fetcher).fetch("https://bbs.ruliweb.com/market/board/1020", timeout);
    }
}
