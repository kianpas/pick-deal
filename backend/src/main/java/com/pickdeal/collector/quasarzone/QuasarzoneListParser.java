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
    private static final String DEAL_PATH = "/bbs/qb_saleinfo/views/";
    private static final Pattern STORE_PREFIX = Pattern.compile("^\\[([^\\]]+)\\]\\s*(.*)$");
    private static final Pattern PRICE_DIGITS = Pattern.compile("([\\d,]+)");
    private static final Pattern BACKGROUND_URL = Pattern.compile("url\\(['\"]?(.*?)['\"]?\\)");

    public List<QuasarzoneDealItem> parse(String html) {
        Document doc = Jsoup.parse(html, BASE_URL);
        rejectChallengePage(doc);
        return doc.select("div.market-info-list, div.v2-list-row.v2-list-row--hotdeal").stream()
                .filter(item -> !isNotice(item))
                // 블라인드 처리된 글은 정상 제목 대신 잠금 문구만 남는다 — 수집 대상이 아니다
                .filter(item -> titleOf(item) != null && !titleOf(item).contains("블라인드 처리된 글"))
                // 새 목록에는 파트너 핫딜이 함께 노출될 수 있으므로 일반 핫딜 URL만 허용한다
                .filter(item -> dealLinkOf(item) != null)
                .map(this::toDealItem)
                .toList();
    }

    private void rejectChallengePage(Document doc) {
        String text = doc.text();
        if (text.contains("Enable JavaScript and cookies to continue")
                || text.contains("보안검사를 완료하세요")
                || doc.selectFirst("#captcha-container") != null) {
            throw new IllegalStateException("퀘이사존 보안검사 페이지가 반환되어 목록을 파싱할 수 없습니다");
        }
    }

    private boolean isNotice(Element item) {
        return NOTICE_LABEL.equals(labelOf(item));
    }

    private QuasarzoneDealItem toDealItem(Element item) {
        Element link = dealLinkOf(item);
        String url = link.absUrl("href");
        String externalId = url.substring(url.lastIndexOf('/') + 1);
        String rawTitle = titleOf(item);
        return new QuasarzoneDealItem(externalId, url, parseStoreName(item, rawTitle), parseTitle(rawTitle),
                parsePrice(item.select("span.text-orange, span.v2-list-row__price").text()),
                categoryOf(item),
                parseThumbnailUrl(item),
                parseCommentCount(item.selectFirst("span.ctn-count")),
                ENDED_LABEL.equals(labelOf(item)) || item.hasClass("is-done"),
                item.select("span.date, span.v2-list-row__time").text().trim());
    }

    private String titleOf(Element item) {
        Element legacyTitle = item.selectFirst("span.ellipsis-with-reply-cnt");
        if (legacyTitle != null) {
            return legacyTitle.text().trim();
        }
        Element link = item.selectFirst("a.subject-link[href*=/bbs/qb_saleinfo/views/]");
        return link == null ? null : link.text().trim();
    }

    private String categoryOf(Element item) {
        Element category = item.selectFirst("span.category");
        if (category == null) {
            category = item.selectFirst("div.v2-list-row__line1 span.v2-badge:not(.v2-partner-badge)");
        }
        return category == null ? "" : category.text().trim();
    }

    private Element dealLinkOf(Element item) {
        return item.select("a[href]").stream()
                .filter(link -> link.absUrl("href").contains(DEAL_PATH))
                .findFirst()
                .orElse(null);
    }

    /** 목록 행의 상태 라벨(공지/진행중/인기/종료) 텍스트. */
    private String labelOf(Element item) {
        return item.select("span.label").text();
    }

    private String parseThumbnailUrl(Element item) {
        Element img = item.selectFirst("img.maxImg");
        if (img != null) {
            return stripCachebuster(img.absUrl("src"));
        }

        String preview = item.attr("data-preview").trim();
        if (!preview.isEmpty()) {
            return stripCachebuster(preview);
        }

        Element thumbnail = item.selectFirst("a.v2-list-row__thumb[style]");
        if (thumbnail == null) {
            return null;
        }
        Matcher matcher = BACKGROUND_URL.matcher(thumbnail.attr("style"));
        return matcher.find() ? stripCachebuster(matcher.group(1)) : null;
    }

    private String stripCachebuster(String url) {
        return url.endsWith("?") ? url.substring(0, url.length() - 1) : url;
    }

    private Integer parseCommentCount(Element count) {
        if (count == null || count.text().isBlank()) {
            return null;
        }
        return Integer.valueOf(count.text().replace(",", "").trim());
    }

    /** 원화 가격만 정수로 변환한다. 통화 정보가 없는 현재 모델에서 USD 등을 원화로 오인하지 않는다. */
    private Long parsePrice(String priceText) {
        if (!isKrw(priceText)) {
            return null;
        }
        Matcher matcher = PRICE_DIGITS.matcher(priceText);
        if (!matcher.find()) {
            return null;
        }
        return Long.parseLong(matcher.group(1).replace(",", ""));
    }

    private boolean isKrw(String priceText) {
        return priceText != null
                && (priceText.contains("￦")
                || priceText.contains("₩")
                || priceText.contains("원")
                || priceText.toUpperCase().contains("KRW"));
    }

    private String parseStoreName(Element item, String rawTitle) {
        Element shopLogo = item.selectFirst("img.v2-shop-logo[alt]");
        if (shopLogo != null && !shopLogo.attr("alt").isBlank()) {
            return shopLogo.attr("alt").trim();
        }
        Element shopText = item.selectFirst("div.v2-list-row__price-group span.v2-list-row__ship");
        if (shopText != null && !shopText.text().isBlank()) {
            return shopText.text().trim();
        }
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private String parseTitle(String rawTitle) {
        Matcher matcher = STORE_PREFIX.matcher(rawTitle);
        return matcher.matches() ? matcher.group(2).trim() : rawTitle.trim();
    }
}
