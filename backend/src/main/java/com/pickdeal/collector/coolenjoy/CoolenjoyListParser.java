package com.pickdeal.collector.coolenjoy;

import com.pickdeal.collector.support.CollectedDeal;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 목록의 가격 전용 칸을 사용한다. 제목·본문 금액은 추측하지 않는다. */
public final class CoolenjoyListParser {
    private static final String BASE = "https://coolenjoy.net";
    private static final Pattern PRICE = Pattern.compile("^([0-9]+|[1-9][0-9]{0,2}(?:,[0-9]{3})+)\\s*원$");
    private static final Pattern STORE = Pattern.compile("^\\[([^\\]]+)]\\s*(.+)$");

    public List<CollectedDeal> parse(String html, OffsetDateTime observedAt) {
        var doc = Jsoup.parse(html, BASE);
        if (doc.selectFirst("input[name=bo_table][value=jirum]") == null
                || doc.selectFirst("ul.na-table") == null)
            throw new IllegalArgumentException("쿨엔조이 지름 게시판 목록이 아님");
        var result = new LinkedHashMap<String, CollectedDeal>();
        var rows = doc.select("ul.na-table > li.d-md-table-row");
        int verified = 0;
        for (var row : rows) {
            var link = row.selectFirst("a.na-subject[href]");
            if (link == null) continue;
            String id = id(link.absUrl("href"));
            String title = link.text().trim();
            if (id == null || title.isBlank()) continue;
            verified++;
            // 고정 공지·프로모션 행은 실제 목록에서 bg-light로 표시된다.
            if (row.hasClass("bg-light") || title.matches("^\\[(공지|광고|홍보)].*")) continue;
            String shop = null;
            var store = STORE.matcher(title);
            if (store.matches()) { shop = store.group(1); title = store.group(2); }
            var category = row.selectFirst("[id=abcd]");
            var comments = row.selectFirst(".count-plus");
            Integer count = null;
            if (comments != null) {
                String text = comments.text().replaceAll("[\\[\\],\\s]", "");
                try { if (text.matches("[0-9]+")) count = Integer.valueOf(text); }
                catch (NumberFormatException ignored) { }
            }
            result.putIfAbsent(id, new CollectedDeal(id, BASE + "/bbs/jirum/" + id, shop, title,
                    price(cell(row, "가격")), category == null ? null : category.text(), count,
                    null, null, date(cell(row, "등록일"), observedAt), null));
            if (result.size() == 50) break;
        }
        if (verified == 0 && (!rows.isEmpty() || !doc.text().contains("게시물이 없습니다")))
            throw new IllegalArgumentException("쿨엔조이 목록의 게시글을 확인하지 못함");
        return List.copyOf(result.values());
    }

    private static String cell(Element row, String label) {
        var labels = row.select("span.sr-only").stream().filter(e -> e.text().equals(label)).toList();
        if (labels.size() != 1) return "";
        var cell = labels.get(0).parent().clone();
        cell.select(".sr-only").remove();
        return cell.text().trim();
    }

    private static Long price(String value) {
        var match = PRICE.matcher(value);
        if (!match.matches()) return null;
        try { return Long.valueOf(match.group(1).replace(",", "")); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String id(String value) {
        try {
            var uri = URI.create(value);
            if (!"https".equals(uri.getScheme()) || !"coolenjoy.net".equals(uri.getHost())
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)) return null;
            var match = Pattern.compile("^/bbs/jirum/([1-9][0-9]{0,19})$").matcher(uri.getPath());
            return match.matches() ? match.group(1) : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private static OffsetDateTime date(String value, OffsetDateTime observedAt) {
        var now = observedAt.atZoneSameInstant(ZoneId.of("Asia/Seoul"));
        try {
            LocalDate day;
            LocalTime time = LocalTime.MIDNIGHT;
            if (value.matches("\\d{2}:\\d{2}")) {
                day = now.toLocalDate();
                time = LocalTime.parse(value);
                if (time.isAfter(now.toLocalTime())) day = day.minusDays(1);
            } else if (value.matches("\\d{2}\\.\\d{2}")) {
                day = LocalDate.parse(now.getYear() + "." + value, DateTimeFormatter.ofPattern("uuuu.MM.dd"));
                if (day.isAfter(now.toLocalDate())) day = day.minusYears(1);
            } else {
                day = LocalDate.parse(value, DateTimeFormatter.ofPattern("yy.MM.dd"));
            }
            return day.atTime(time).atZone(now.getZone()).toOffsetDateTime();
        } catch (RuntimeException ignored) { return null; }
    }
}
