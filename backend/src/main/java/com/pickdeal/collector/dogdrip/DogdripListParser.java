package com.pickdeal.collector.dogdrip;

import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.CategoryNormalizer;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 목록 외 영역과 공지를 제외한다. 외부 요청 없는 순수 파서. */
public class DogdripListParser {
    private static final Pattern SHOP = Pattern.compile("^\\[([^]\\r\\n]+)]\\s*(.+)$");
    private static final Pattern PRICE = Pattern.compile("(?:^|\\s)([0-9]+(?:,[0-9]{3})*)원(?:\\s*\\([^()]*\\))?\\s*$");
    private static final Pattern AGO = Pattern.compile("^(\\d+)\\s*(분|시간|일)\\s*전$");

    public List<CollectedDeal> parse(String html, OffsetDateTime now) {
        var doc = Jsoup.parse(html, DogdripClient.LIST_URL);
        if (!doc.title().contains("핫딜") || html.contains("cf-chl-") || html.contains("challenge-platform"))
            throw new IllegalArgumentException("개드립 정상 목록이 아님");
        var result = new LinkedHashMap<String, CollectedDeal>();
        for (Element row : doc.select("ul.ed.list li.webzine:not(.notice)")) {
            Element link = row.selectFirst("a.title-link[data-document-srl]");
            if (link == null) continue;
            String id = link.attr("data-document-srl");
            if (!id.matches("[1-9][0-9]{0,19}") || !validLink(link.absUrl("href"), id)) continue;
            String title = link.text().trim();
            if (title.isEmpty()) continue;
            String shop = null;
            var shopMatch = SHOP.matcher(title);
            if (shopMatch.matches()) { shop = shopMatch.group(1).trim(); title = shopMatch.group(2).trim(); }
            Long price = null;
            var priceMatch = PRICE.matcher(title);
            // 클릭 적립액·쿠폰 혜택은 판매가가 아니다. 모호하면 가격을 비워 둔다.
            if (priceMatch.find() && !title.contains("일일적립") && !title.contains("클릭")
                    && !title.contains("캐시백") && !title.contains("쿠폰팩")) {
                try { price = Long.valueOf(priceMatch.group(1).replace(",", "")); }
                catch (NumberFormatException ignored) { /* 모호하거나 범위 초과인 가격은 null */ }
            }
            Element category = row.selectFirst(".list-meta span.text-muted");
            String categoryText = category == null ? null : category.text();
            if (categoryText != null && categoryText.startsWith("[") && categoryText.endsWith("]"))
                categoryText = categoryText.substring(1, categoryText.length() - 1);
            else categoryText = null;
            Integer comments = null;
            Element count = row.selectFirst("h5.title > span.text-primary");
            if (count != null && count.text().matches("[0-9]+")) {
                try { comments = Integer.valueOf(count.text()); } catch (NumberFormatException ignored) {}
            }
            Element img = row.selectFirst("img.webzine-thumbnail[src]");
            String thumb = img == null ? null : safeImage(img.absUrl("src"));
            Element clock = row.selectFirst(".list-meta .fa-clock");
            OffsetDateTime posted = clock == null ? null : postedAt(clock.parent().text(), now);
            Boolean ended = link.attr("style").contains("line-through") ? Boolean.TRUE : null;
            result.putIfAbsent(id, new CollectedDeal(id, "https://www.dogdrip.net/" + id, shop, title,
                    price, CategoryNormalizer.normalize(categoryText), comments, thumb, ended, posted, null));
            if (result.size() >= 50) break;
        }
        if (result.isEmpty()) throw new IllegalArgumentException("개드립 게시글을 확인하지 못함");
        return List.copyOf(result.values());
    }

    private boolean validLink(String url, String id) {
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme()) && "www.dogdrip.net".equals(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1 && uri.getQuery() == null
                    && uri.getFragment() == null && uri.getPath().equals("/" + id);
        } catch (IllegalArgumentException e) { return false; }
    }

    private String safeImage(String url) {
        try {
            URI uri = URI.create(url);
            return List.of("https", "http").contains(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null ? url : null;
        } catch (RuntimeException e) { return null; }
    }

    private OffsetDateTime postedAt(String text, OffsetDateTime now) {
        var match = AGO.matcher(text.trim());
        try {
            if (match.matches()) {
                long value = Long.parseLong(match.group(1));
                return switch (match.group(2)) {
                    case "분" -> now.minusMinutes(value);
                    case "시간" -> now.minusHours(value);
                    default -> now.minusDays(value);
                };
            }
        } catch (RuntimeException ignored) {}
        return null;
    }
}
