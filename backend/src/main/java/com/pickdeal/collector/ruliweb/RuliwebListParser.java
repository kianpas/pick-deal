package com.pickdeal.collector.ruliweb;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 루리웹 핫딜 게시판 목록 HTML을 파싱한다. 네트워크 접근 없는 순수 변환.
 */
public class RuliwebListParser {

    private static final String BASE_URL = "https://bbs.ruliweb.com";
    private static final String ENDED_BADGE = "[종료]";
    private static final Pattern READ_ID = Pattern.compile("/read/(\\d+)");
    private static final Pattern STORE_PREFIX = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");
    private static final Pattern COMMENT_COUNT = Pattern.compile("\\d+");
    /**
     * 제목 끝의 가격 관례. 작성자마다 표기가 달라 <b>끝에 오는 명확한 형태만</b> 인정한다.
     * 예: "... / 730900원", "... 727,725원", "... / 648000"
     *
     * <p>괄호 안 표기("(12,940원~/무료)")나 잘린 제목("(15,370...")은 가격 위치가
     * 불분명해 제외한다 — 틀린 가격은 없는 가격보다 나쁘다.
     * 슬래시 뒤 '원'이 없는 형태는 날짜 조각("8/26")과 구분되도록 3자리 이상만 받는다.
     */
    private static final Pattern TRAILING_PRICE_WITH_WON = Pattern.compile("([\\d,]+)\\s*원\\s*$");
    private static final Pattern TRAILING_PRICE_AFTER_SLASH = Pattern.compile("/\\s*(\\d[\\d,]{2,})\\s*$");

    public List<RuliwebDealItem> parse(String html) {
        Document doc = Jsoup.parse(html, BASE_URL);
        // 공지(notice)와 BEST 행은 아래 일반 목록과 중복 노출이라 제외한다
        return doc.select("tr.table_body:not(.notice):not(.best)").stream()
                .map(this::toDealItem)
                .filter(Objects::nonNull)
                .toList();
    }

    private RuliwebDealItem toDealItem(Element row) {
        Element link = row.selectFirst("td.subject a.subject_link");
        if (link == null) {
            return null;
        }
        String url = link.absUrl("href");
        Matcher readId = READ_ID.matcher(url);
        if (!readId.find()) {
            return null;
        }

        // 제목은 링크의 직접 텍스트만 — 자식 요소인 아이콘·댓글 수를 제외한다
        String rawTitle = link.ownText().trim();
        return new RuliwebDealItem(
                readId.group(1),
                // 목록 링크 끝에 붙는 빈 쿼리("?")를 떼어 원문 URL을 안정적으로 만든다
                url.endsWith("?") ? url.substring(0, url.length() - 1) : url,
                parseStoreName(rawTitle),
                parseTitle(rawTitle),
                parsePrice(rawTitle),
                row.select("td.divsn").text().trim(),
                parseCommentCount(row.selectFirst("td.subject .num_reply")),
                row.select("td.subject").text().contains(ENDED_BADGE),
                row.select("td.time").text().trim()
        );
    }

    private String parseStoreName(String rawTitle) {
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private String parseTitle(String rawTitle) {
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(2).trim() : rawTitle;
    }

    /** 제목 끝 가격 관례에서 금액을 뽑는다. 관례를 따르지 않으면 null. */
    private Long parsePrice(String rawTitle) {
        Matcher withWon = TRAILING_PRICE_WITH_WON.matcher(rawTitle);
        if (withWon.find()) {
            return toAmount(withWon.group(1));
        }
        Matcher afterSlash = TRAILING_PRICE_AFTER_SLASH.matcher(rawTitle);
        if (afterSlash.find()) {
            return toAmount(afterSlash.group(1));
        }
        return null;
    }

    private Long toAmount(String digits) {
        String normalized = digits.replace(",", "");
        return normalized.isEmpty() ? null : Long.parseLong(normalized);
    }

    private Integer parseCommentCount(Element count) {
        if (count == null) {
            return null;
        }
        Matcher matcher = COMMENT_COUNT.matcher(count.text());
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }
}
