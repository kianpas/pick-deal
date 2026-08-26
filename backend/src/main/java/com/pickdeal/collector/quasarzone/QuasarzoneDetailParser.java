package com.pickdeal.collector.quasarzone;

import com.pickdeal.collector.support.CollectedProductInfo;
import com.pickdeal.collector.support.ExternalUrlSupport;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/** 퀘이사존 상세 정보 표의 판매처와 구매 링크를 파싱한다. */
public class QuasarzoneDetailParser {

    public CollectedProductInfo parse(String html) {
        Document doc = Jsoup.parse(html);
        String shopName = valueForHeader(doc, "판매처");
        String productUrl = ExternalUrlSupport.httpUrlOrNull(valueForHeader(doc, "링크"));
        if (isQuasarzoneUrl(productUrl)) {
            productUrl = null;
        }
        if (productUrl == null) {
            productUrl = bodyProductUrl(doc);
        }
        return new CollectedProductInfo(blankToNull(shopName), productUrl);
    }

    /**
     * 상세 표에 `링크` 행이 없는 글의 폴백. 본문 원본은 {@code textarea#org_contents}에 들어 있고
     * 화면의 본문 영역은 스크립트가 채우기 전까지 비어 있다.
     *
     * <p>페이지의 배너·후원사 링크와 섞이지 않도록 <b>본문 안</b>으로 범위를 좁히고,
     * 그 안의 첫 외부 링크만 쓴다(작성자가 구매 링크를 먼저 적는 관례).
     */
    private String bodyProductUrl(Document doc) {
        Element body = doc.selectFirst("textarea#org_contents");
        if (body == null) {
            return null;
        }
        return Jsoup.parse(body.val()).select("a[href]").stream()
                .map(link -> link.attr("href"))
                .map(ExternalUrlSupport::httpUrlOrNull)
                .filter(url -> url != null && !isQuasarzoneUrl(url))
                .findFirst()
                .orElse(null);
    }

    private String valueForHeader(Document doc, String label) {
        return doc.select("tr").stream()
                .filter(row -> {
                    Element header = row.selectFirst("th");
                    return header != null && label.equals(header.text().trim());
                })
                .map(row -> row.selectFirst("td"))
                .filter(value -> value != null)
                .map(Element::text)
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private boolean isQuasarzoneUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            String host = URI.create(url).getHost();
            return host != null && (host.equals("quasarzone.com") || host.endsWith(".quasarzone.com"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
