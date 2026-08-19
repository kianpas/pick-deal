package com.pickdeal.collector.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * 출처 사이트의 HTML을 가져온다. 출처별 Client가 URL만 정하고 요청 방식은 여기로 모은다.
 *
 * <p>기본 User-Agent는 차단하는 사이트가 있어 브라우저 UA를 쓴다(docs/notes 2026-07-09).
 */
@Component
public class HtmlFetcher {

    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 10_000;

    public String fetch(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(BROWSER_USER_AGENT)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(TIMEOUT_MS)
                    .get()
                    .outerHtml();
        } catch (IOException e) {
            throw new UncheckedIOException("목록 요청 실패: " + url, e);
        }
    }
}
