package com.pickdeal.collector.coolenjoy;

import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.ExternalUrlSupport;
import java.io.StringReader;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jsoup.Jsoup;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** 외부 요청 없는 RSS 2.0 파서. 본문 HTML은 이미지·분리된 상품 링크 추출에만 사용한다. */
public final class CoolenjoyRssParser {
    private static final String BOARD_URL = "https://coolenjoy.net/bbs/jirum";
    private static final String DC = "http://purl.org/dc/elements/1.1/";
    private static final String AMOUNT = "([0-9]+|[1-9][0-9]{0,2}(?:,[0-9]{3})+)";
    private static final Pattern PRICE = Pattern.compile("(?:\\(" + AMOUNT
            + "\\s*원(?:\\s*/\\s*(?:무료배송|무료|무배))?\\)|(?:^|\\s)" + AMOUNT
            + "\\s*원(?:\\s*\\((?:무료배송|무료|무배)\\))?)\\s*$");
    private static final Pattern AMBIGUOUS_PRICE = Pattern.compile("적립|캐시백|환급|체감|쿠폰팩|원\\s*할인|카드|멤버십|첫구매|첫구입|월\\s*요금|\\$|USD|달러");
    private static final Pattern SKIP_TITLE = Pattern.compile("^\\[(?:공지|광고|홍보)]");

    public List<CollectedDeal> parse(String xml) {
        Element root;
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override public void error(SAXParseException e) throws SAXException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            root = builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (Exception e) {
            throw new IllegalArgumentException("쿨엔조이 정상 RSS XML이 아님", e);
        }
        var channels = children(root, "channel");
        if (!"rss".equals(root.getTagName()) || !"2.0".equals(root.getAttribute("version")) || channels.size() != 1)
            throw new IllegalArgumentException("쿨엔조이 RSS 채널이 아님");
        var channel = channels.get(0);
        if (!BOARD_URL.equals(text(channel, "link"))) throw new IllegalArgumentException("쿨엔조이 지름 게시판 피드가 아님");
        var items = children(channel, "item");
        var result = new LinkedHashMap<String, CollectedDeal>();
        int verified = 0;
        for (var item : items) {
            String id = externalId(text(item, "link"));
            String title = text(item, "title");
            if (id == null || title.isBlank()) continue;
            verified++;
            if (SKIP_TITLE.matcher(title).find()) continue;
            var body = Jsoup.parseBodyFragment(text(item, "description"), BOARD_URL);
            body.select("script, style, iframe, object, embed").remove();
            String thumbnail = null;
            for (var image : body.select("img[src]")) {
                thumbnail = safeUrl(image.absUrl("src"));
                if (thumbnail != null) break;
            }
            // 실제 RSS의 '구분선 → URL만 표시하는 문단'만 사용한다. 복수 후보는 추측하지 않는다.
            var productLinks = new LinkedHashSet<String>();
            for (var link : body.select("hr + p > a[href]")) {
                String url = safeUrl(link.absUrl("href"));
                if (url != null && link.text().trim().equals(link.attr("href").trim()) && externalHost(url))
                    productLinks.add(url);
            }
            result.putIfAbsent(id, new CollectedDeal(id, BOARD_URL + "/" + id, null, title,
                    price(title), null, null, thumbnail, null, postedAt(item),
                    productLinks.size() == 1 ? productLinks.iterator().next() : null));
            if (result.size() == 50) break;
        }
        if (!items.isEmpty() && verified == 0) throw new IllegalArgumentException("쿨엔조이 RSS 게시글을 확인하지 못함");
        return List.copyOf(result.values());
    }

    private static List<Element> children(Element parent, String name) {
        var result = new ArrayList<Element>();
        var nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && element.getTagName().equals(name)) result.add(element);
        }
        return result;
    }

    private static String text(Element parent, String name) {
        var values = children(parent, name);
        return values.size() == 1 ? values.get(0).getTextContent().trim() : "";
    }

    private static String externalId(String url) {
        try {
            var uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"coolenjoy.net".equals(uri.getHost())
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                    || uri.getQuery() != null || uri.getFragment() != null) return null;
            var match = Pattern.compile("^/bbs/jirum/([1-9][0-9]{0,19})$").matcher(uri.getPath());
            return match.matches() ? match.group(1) : null;
        } catch (RuntimeException ignored) { return null; }
    }

    private static OffsetDateTime postedAt(Element item) {
        var values = item.getElementsByTagNameNS(DC, "date");
        if (values.getLength() != 1) return null;
        try { return OffsetDateTime.parse(values.item(0).getTextContent().trim(), DateTimeFormatter.RFC_1123_DATE_TIME); }
        catch (RuntimeException ignored) { return null; }
    }

    private static Long price(String title) {
        if (AMBIGUOUS_PRICE.matcher(title).find()) return null;
        if (Pattern.compile("[0-9][0-9,]*\\s*원").matcher(title).results().count() != 1) return null;
        var match = PRICE.matcher(title);
        if (!match.find()) return null;
        try { return Long.valueOf((match.group(1) != null ? match.group(1) : match.group(2)).replace(",", "")); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String safeUrl(String value) {
        String url = ExternalUrlSupport.httpUrlOrNull(value);
        return url != null && URI.create(url).getUserInfo() == null ? url : null;
    }

    private static boolean externalHost(String url) {
        String host = URI.create(url).getHost().toLowerCase(java.util.Locale.ROOT);
        return !host.equals("coolenjoy.net") && !host.endsWith(".coolenjoy.net")
                && !host.equals("coolenjoy.co.kr") && !host.endsWith(".coolenjoy.co.kr");
    }
}
