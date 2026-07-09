package com.pickdeal.collector.quasarzone;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 퀘이사존 핫딜 게시판 목록 HTML을 파싱한다. 네트워크 접근 없는 순수 변환.
 */
public class QuasarzoneListParser {

    private static final String BASE_URL = "https://quasarzone.com";
    private static final String NOTICE_LABEL = "공지";
    private static final String ENDED_LABEL = "종료";
    private static final Pattern STORE_PREFIX = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");
    private static final Pattern PRICE_DIGITS = Pattern.compile("([\\d,]+)");

    public List<QuasarzoneDealItem> parse(String html) {
        Document doc = Jsoup.parse(html, BASE_URL);
        return doc.select("div.market-info-list").stream()
                .filter(item -> !isNotice(item))
                // 블라인드 처리된 글은 제목 span 없이 잠금 문구만 남는다 — 수집 대상이 아니다
                .filter(item -> item.selectFirst("span.ellipsis-with-reply-cnt") != null)
                .map(this::toDealItem)
                .toList();
    }

    private boolean isNotice(Element item) {
        return NOTICE_LABEL.equals(labelOf(item));
    }

    private QuasarzoneDealItem toDealItem(Element item) {
        Element link = item.selectFirst("a.subject-link");
        String url = link.absUrl("href");
        String externalId = url.substring(url.lastIndexOf('/') + 1);
        String rawTitle = item.select("span.ellipsis-with-reply-cnt").text();
        return new QuasarzoneDealItem(externalId, url, parseStoreName(rawTitle), parseTitle(rawTitle),
                parsePrice(item.select("span.text-orange").text()),
                item.select("span.category").text(),
                parseThumbnailUrl(item),
                ENDED_LABEL.equals(labelOf(item)),
                item.select("span.date").text().trim());
    }

    /** 목록 행의 상태 라벨(공지/진행중/인기/종료) 텍스트. */
    private String labelOf(Element item) {
        return item.select("span.label").text();
    }

    private String parseThumbnailUrl(Element item) {
        Element img = item.selectFirst("img.maxImg");
        if (img == null) {
            return null;
        }
        // src 끝에 캐시버스터용 '?'가 붙어 오는 경우가 있어 제거한다
        String src = img.absUrl("src");
        return src.endsWith("?") ? src.substring(0, src.length() - 1) : src;
    }

    /** 예: "￦ 36,000 (KRW)", "￦ 10,850원 (KRW)", "￦ 0 (KRW)" → 금액 정수. 형식이 다르면 null. */
    private Long parsePrice(String priceText) {
        Matcher matcher = PRICE_DIGITS.matcher(priceText);
        if (!matcher.find()) {
            return null;
        }
        return Long.parseLong(matcher.group(1).replace(",", ""));
    }

    private String parseStoreName(String rawTitle) {
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private String parseTitle(String rawTitle) {
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(2).trim() : rawTitle.trim();
    }
}
