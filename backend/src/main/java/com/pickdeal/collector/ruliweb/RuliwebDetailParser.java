package com.pickdeal.collector.ruliweb;

import com.pickdeal.collector.support.CollectedProductInfo;
import com.pickdeal.collector.support.ExternalUrlSupport;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 루리웹 상세의 출처 URL 영역에서 구매 링크를 파싱한다. */
public class RuliwebDetailParser {

    private static final String BASE_URL = "https://bbs.ruliweb.com";

    public CollectedProductInfo parse(String html) {
        Document doc = Jsoup.parse(html, BASE_URL);
        Element link = doc.selectFirst(".source_url a");
        return new CollectedProductInfo(null, productUrl(link));
    }

    private String productUrl(Element link) {
        if (link == null) {
            return null;
        }
        String displayedUrl = ExternalUrlSupport.httpUrlOrNull(link.text());
        if (displayedUrl != null) {
            return displayedUrl;
        }

        String href = link.absUrl("href");
        String redirectedUrl = ruliwebDestination(href);
        if (redirectedUrl != null) {
            return redirectedUrl;
        }
        return isRuliwebUrl(href) ? null : ExternalUrlSupport.httpUrlOrNull(href);
    }

    private String ruliwebDestination(String href) {
        try {
            URI uri = URI.create(href);
            if (!isRuliwebHost(uri.getHost()) || uri.getRawQuery() == null) {
                return null;
            }
            return Arrays.stream(uri.getRawQuery().split("&"))
                    .map(part -> part.split("=", 2))
                    .filter(pair -> pair.length == 2 && "ol".equals(pair[0]))
                    .map(pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8))
                    .map(ExternalUrlSupport::httpUrlOrNull)
                    .filter(value -> value != null)
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isRuliwebUrl(String url) {
        try {
            return isRuliwebHost(URI.create(url).getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isRuliwebHost(String host) {
        return host != null && (host.equals("ruliweb.com") || host.endsWith(".ruliweb.com"));
    }
}
