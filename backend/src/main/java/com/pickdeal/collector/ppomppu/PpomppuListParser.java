package com.pickdeal.collector.ppomppu;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 네트워크·DB 없는 목록 파서. 자동 수집기에는 아직 등록하지 않는다. */
public final class PpomppuListParser {
    public static final String BASE_URL = "https://www.ppomppu.co.kr/zboard/";
    // 원화 단위와 가격/배송비 구분이 명시된 마지막 괄호만 인정한다.
    private static final Pattern PRICE = Pattern.compile(
            "\\(([0-9]+|[1-9][0-9]{0,2}(?:,[0-9]{3})+)\\s*원\\s*/[^()]+\\)\\s*$");

    public List<PpomppuDealItem> parse(String html) {
        var document = Jsoup.parse(html, BASE_URL);
        var rows = document.select("tr.baseList:not(.baseNotice)");
        if (rows.isEmpty()) throw new IllegalArgumentException("Ppomppu list markup not found");
        var items = new LinkedHashMap<String, PpomppuDealItem>();
        for (Element row : rows) {
            Element link = row.selectFirst("a.baseList-title[href]");
            Element number = row.selectFirst(".baseList-numb");
            if (link == null || number == null) continue;
            String id = externalId(link.absUrl("href"));
            // 공지·쇼핑뽐뿌 광고·다른 게시판 추천 영역은 번호와 게시판으로 제외한다.
            if (id == null || !number.text().equals(id)) continue;
            Element titleNode = link.clone();
            Element prefix = titleNode.selectFirst(".subject_preface");
            String store = prefix == null ? null : unbracket(prefix.text());
            if (prefix != null) prefix.remove();
            String title = titleNode.text().trim();
            if (title.isEmpty()) continue;
            Element time = row.selectFirst("time.baseList-time");
            String postedAt = time == null ? null : time.parent().attr("title").trim();
            if (postedAt != null && postedAt.isEmpty()) postedAt = time.text();
            Element image = row.selectFirst("a.baseList-thumb img[src]");
            String thumbnail = image == null ? null : httpUrl(image.absUrl("src"));
            if (thumbnail != null && thumbnail.contains("/noimage/")) thumbnail = null;
            items.putIfAbsent(id, new PpomppuDealItem(id, BASE_URL + "view.php?id=ppomppu&no=" + id,
                    store, title, price(title), unbracket(row.select(".baseList-small").text()),
                    count(row.selectFirst(".baseList-c")), thumbnail, blankToNull(postedAt)));
        }
        if (items.isEmpty()) throw new IllegalArgumentException("No verified Ppomppu deal rows");
        return List.copyOf(items.values());
    }

    private static String externalId(String url) {
        try {
            URI uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"www.ppomppu.co.kr".equals(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null
                    || !"/zboard/view.php".equals(uri.getPath()) || uri.getRawQuery() == null) return null;
            var params = new LinkedHashMap<String, String>();
            for (String part : uri.getRawQuery().split("&")) {
                if (part.isEmpty()) continue;
                String[] pair = part.split("=", 2);
                if (pair.length != 2 || params.putIfAbsent(pair[0], pair[1]) != null) return null;
            }
            String id = params.get("no");
            return "ppomppu".equals(params.get("id")) && id != null && id.matches("[1-9][0-9]*") ? id : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Long price(String title) {
        var matcher = PRICE.matcher(title);
        if (!matcher.find()) return null;
        try { return Long.valueOf(matcher.group(1).replace(",", "")); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static Integer count(Element element) {
        if (element == null) return null; // 표시 없음은 0으로 추측하지 않는다.
        String text = element.text().replace(",", "").trim();
        if (!text.matches("[0-9]+")) return null;
        try { return Integer.valueOf(text); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String httpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }

    private static String unbracket(String text) {
        String value = text.trim();
        if (value.startsWith("[") && value.endsWith("]")) value = value.substring(1, value.length() - 1).trim();
        return blankToNull(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
