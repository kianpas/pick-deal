package com.pickdeal.collector.coolenjoy;

import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.ExternalUrlSupport;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;

/** 실제 게시글 영역만 사용한다. 관련 링크의 표시 URL을 읽으며 경유 링크를 요청하지 않는다. */
public final class CoolenjoyDetailParser {
    public CollectedDeal enrich(String html, CollectedDeal deal) {
        if (html == null) throw new IllegalArgumentException("쿨엔조이 상세 응답 없음");
        var doc = Jsoup.parse(html, deal.url());
        var identity = doc.selectFirst("meta[property=og:url]");
        var titleElement = doc.selectFirst("#bo_v > header #bo_v_title");
        var body = doc.selectFirst("#bo_v #bo_v_con .view-content");
        if (identity == null || !deal.url().equals(identity.attr("content")) || titleElement == null || body == null)
            throw new IllegalArgumentException("쿨엔조이 요청 게시글의 정상 상세가 아님");

        var heading = titleElement.clone();
        boolean hasCategory = heading.select(".sr-only").stream().anyMatch(e -> e.text().equals("분류"));
        heading.select(".sr-only").remove();
        String title = heading.text().trim();
        if (hasCategory && title.contains("|")) title = title.substring(title.indexOf('|') + 1).trim();
        String shop = deal.storeName();
        var store = Pattern.compile("^\\[([^\\]]+)]\\s*(.+)$").matcher(title);
        if (store.matches()) { shop = store.group(1); title = store.group(2); }
        if (title.isBlank() || title.length() > 300) title = deal.title();

        String thumbnail = deal.thumbnailUrl();
        for (var img : body.select("img[src]")) {
            String candidate = safeUrl(img.absUrl("src"));
            if (candidate != null) { thumbnail = candidate; break; }
        }

        var links = new LinkedHashSet<String>();
        for (var anchor : doc.select("#bo_v_atc > ul.na-table > li a[href]")) {
            String href = anchor.absUrl("href");
            String candidate = null;
            // 링크 클릭 집계용 내부 주소의 wr_id가 현재 게시글과 일치해야 한다.
            if (href.matches("https://coolenjoy\\.net/bbs/link2?\\.php\\?bo_table=jirum&wr_id="
                    + deal.externalId() + "&no=[0-9]+")) {
                var text = anchor.clone();
                text.select(".count-plus, .sr-only").remove();
                candidate = safeUrl(text.text());
            } else if (safeUrl(href) != null && external(href)) {
                candidate = href;
            }
            if (candidate != null && external(candidate) && !candidate.contains("…") && !candidate.contains("..."))
                links.add(candidate);
        }
        String productUrl = links.size() == 1 ? links.iterator().next() : deal.productUrl();
        OffsetDateTime postedAt = deal.postedAt();
        var posted = doc.selectFirst("#bo_v_info time[datetime]");
        if (posted != null) {
            try { postedAt = OffsetDateTime.parse(posted.attr("datetime")); }
            catch (RuntimeException ignored) { }
        }
        return new CollectedDeal(deal.externalId(), deal.url(), shop, title, deal.price(), deal.category(),
                deal.commentCount(), thumbnail, deal.ended(), postedAt, productUrl, deal.currency());
    }

    private static String safeUrl(String value) {
        String url = ExternalUrlSupport.httpUrlOrNull(value);
        return url != null && URI.create(url).getUserInfo() == null ? url : null;
    }

    private static boolean external(String value) {
        String host = URI.create(value).getHost().toLowerCase(Locale.ROOT);
        return !host.equals("coolenjoy.net") && !host.endsWith(".coolenjoy.net")
                && !host.equals("coolenjoy.co.kr") && !host.endsWith(".coolenjoy.co.kr");
    }
}
