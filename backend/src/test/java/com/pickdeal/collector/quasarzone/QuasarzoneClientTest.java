package com.pickdeal.collector.quasarzone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pickdeal.collector.support.HtmlFetcher;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class QuasarzoneClientTest {

    @Test
    @DisplayName("페이지 URL과 출처별 timeout을 HTML 요청에 적용한다")
    void fetchesConfiguredPageWithTimeout() {
        HtmlFetcher fetcher = Mockito.mock(HtmlFetcher.class);
        Duration timeout = Duration.ofSeconds(7);
        QuasarzoneCollectorProperties properties =
                new QuasarzoneCollectorProperties(true, timeout, 2, 40, 3, 150, 3);
        QuasarzoneClient client = new QuasarzoneClient(fetcher, properties);
        given(fetcher.fetch("https://quasarzone.com/bbs/qb_saleinfo?page=2", timeout))
                .willReturn("<html>page 2</html>");

        String html = client.fetchListHtml(2);

        assertThat(html).isEqualTo("<html>page 2</html>");
        verify(fetcher).fetch("https://quasarzone.com/bbs/qb_saleinfo?page=2", timeout);
    }

    @Test
    @DisplayName("상세 URL에도 출처별 timeout을 적용한다")
    void fetchesDetailWithTimeout() {
        HtmlFetcher fetcher = Mockito.mock(HtmlFetcher.class);
        Duration timeout = Duration.ofSeconds(7);
        QuasarzoneCollectorProperties properties =
                new QuasarzoneCollectorProperties(true, timeout, 1, 50, 3, 150, 3);
        QuasarzoneClient client = new QuasarzoneClient(fetcher, properties);
        String url = "https://quasarzone.com/bbs/qb_saleinfo/views/1";
        given(fetcher.fetch(url, timeout)).willReturn("<html>detail</html>");

        assertThat(client.fetchDetailHtml(url)).isEqualTo("<html>detail</html>");
        verify(fetcher).fetch(url, timeout);
    }
}
