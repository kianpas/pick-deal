package com.pickdeal.collector.quasarzone;

import com.pickdeal.collector.support.HtmlFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 퀘이사존 핫딜 게시판 목록 HTML을 가져온다 (파이프라인의 fetch 단계).
 */
@Component
@RequiredArgsConstructor
public class QuasarzoneClient {

    private static final String LIST_URL = "https://quasarzone.com/bbs/qb_saleinfo";

    private final HtmlFetcher htmlFetcher;
    private final QuasarzoneCollectorProperties properties;

    public String fetchListHtml(int page) {
        return htmlFetcher.fetch(pageUrl(page), properties.timeout());
    }

    private String pageUrl(int page) {
        return page == 1 ? LIST_URL : LIST_URL + "?page=" + page;
    }
}
