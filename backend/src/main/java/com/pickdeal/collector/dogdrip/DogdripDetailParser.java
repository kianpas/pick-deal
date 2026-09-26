package com.pickdeal.collector.dogdrip;

import com.pickdeal.collector.support.CollectedProductInfo;
import com.pickdeal.collector.support.ExternalUrlSupport;
import java.net.URI;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 구매 정보 표의 링크만 사용한다. 댓글·광고·일반 본문 링크는 상품으로 추측하지 않는다. */
public class DogdripDetailParser {
    public CollectedProductInfo parse(String html) {
        var doc = Jsoup.parse(html);
        if (html.contains("cf-chl-") || html.contains("challenge-platform")) {
            throw new IllegalArgumentException("개드립 정상 상세가 아님");
        }
        for (Element row : doc.select("table.ed.extra-value tr")) {
            Element header = row.selectFirst("th");
            if (header == null) continue;
            Element label = header.clone();
            label.select(".tooltip").remove();
            if (!"링크".equals(label.text().trim())) continue;
            for (Element link : row.select("td a[href]")) {
                String url = ExternalUrlSupport.httpUrlOrNull(link.attr("href"));
                if (url == null) continue;
                URI uri = URI.create(url);
                String host = uri.getHost().toLowerCase(java.util.Locale.ROOT);
                if (uri.getUserInfo() == null && !host.equals("dogdrip.net") && !host.endsWith(".dogdrip.net")) {
                    return new CollectedProductInfo(null, url);
                }
            }
        }
        return new CollectedProductInfo(null, null);
    }
}
