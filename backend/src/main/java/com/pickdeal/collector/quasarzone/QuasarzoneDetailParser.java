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
        return new CollectedProductInfo(blankToNull(shopName), productUrl);
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
