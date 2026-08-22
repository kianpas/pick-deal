package com.pickdeal.collector.ruliweb;

import com.pickdeal.collector.support.HtmlFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 루리웹 핫딜 게시판 목록 HTML을 가져온다 (파이프라인의 fetch 단계).
 */
@Component
@RequiredArgsConstructor
public class RuliwebClient {

    private static final String LIST_URL = "https://bbs.ruliweb.com/market/board/1020";

    private final HtmlFetcher htmlFetcher;
    private final RuliwebCollectorProperties properties;

    public String fetchListHtml(int page) {
        return htmlFetcher.fetch(pageUrl(page), properties.timeout());
    }

    private String pageUrl(int page) {
        return page == 1 ? LIST_URL : LIST_URL + "?page=" + page;
    }
}
