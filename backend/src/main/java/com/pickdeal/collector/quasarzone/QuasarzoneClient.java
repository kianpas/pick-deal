package com.pickdeal.collector.quasarzone;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

/**
 * 핫딜 게시판 목록 HTML을 가져오는 HTTP 클라이언트 (파이프라인의 fetch 단계).
 * 기본 UA는 차단되므로 브라우저 User-Agent가 필수다 (docs/notes 2026-07-09).
 */
@Component
public class QuasarzoneClient {

    private static final String LIST_URL = "https://quasarzone.com/bbs/qb_saleinfo";
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/126.0.0.0 Safari/537.36";

    public String fetchListHtml() {
        try {
            return Jsoup.connect(LIST_URL)
                    .userAgent(BROWSER_USER_AGENT)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(10_000)
                    .get()
                    .outerHtml();
        } catch (IOException e) {
            throw new UncheckedIOException("핫딜 목록 요청 실패: " + LIST_URL, e);
        }
    }
}
