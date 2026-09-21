package com.pickdeal.deal.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** 교차 출처 후보 탐색에 사용할 보수적인 제목·판매몰 정규화 규칙. */
public final class DealMatchNormalizer {

    private static final Map<String, String> SHOP_ALIASES = Map.of(
            "g마켓", "지마켓", "네이버쇼핑", "네이버");
    // 무료배송만 처리한다. 카드/멤버십/조건부 배송 문구는 구매 조건이므로 보존한다.
    private static final String AMOUNT = "([0-9]+(?:,[0-9]{3})*)";
    private static final Pattern PRICE_AND_SHIPPING = Pattern.compile(
            "\\s*\\(\\s*" + AMOUNT + "\\s*원\\s*/\\s*(?:무료|무배|무료배송)\\s*\\)\\s*$");
    private static final Pattern PRICE_THEN_SHIPPING = Pattern.compile(
            "\\s+" + AMOUNT + "\\s*원\\s*\\(\\s*(?:무료|무배|무료배송)\\s*\\)\\s*$");

    private DealMatchNormalizer() {
    }

    /** 판매몰 말머리만 제거하고 모델·용량·수량을 포함한 나머지 글자는 모두 보존한다. */
    public static String normalizeTitle(String title, String shopName) {
        return normalizeTitle(title, shopName, null);
    }

    public static String normalizeTitle(String title, String shopName, Long price) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String normalizedTitle = normalizeUnicode(title).trim();
        String normalizedShop = shopName == null ? null : normalizeUnicode(shopName).trim();
        if (normalizedShop != null && !normalizedShop.isBlank()) {
            String shopPrefix = "[" + normalizedShop + "]";
            if (normalizedTitle.regionMatches(true, 0, shopPrefix, 0, shopPrefix.length())) {
                normalizedTitle = normalizedTitle.substring(shopPrefix.length()).trim();
            }
        }

        normalizedTitle = removeVerifiedPriceSuffix(normalizedTitle, price);
        String result = normalizedTitle
                .toLowerCase(Locale.ROOT)
                // 숫자 사이 소수점은 용량·모델 구분에 필요하다(1.5L != 15L).
                .replaceAll("(?<!\\p{N})\\.|\\.(?!\\p{N})|[^\\p{L}\\p{N}.]", "");
        return result.isBlank() ? null : result;
    }

    public static String titleHash(String title, String shopName) {
        return titleHash(title, shopName, null);
    }

    public static String titleHash(String title, String shopName, Long price) {
        String normalizedTitle = normalizeTitle(title, shopName, price);
        if (normalizedTitle == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(normalizedTitle.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static String normalizeShopName(String shopName) {
        if (shopName == null || shopName.isBlank()) {
            return null;
        }
        String result = normalizeUnicode(shopName)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return result.isBlank() ? null : SHOP_ALIASES.getOrDefault(result, result);
    }

    private static String removeVerifiedPriceSuffix(String title, Long price) {
        if (price == null) {
            return title;
        }
        for (Pattern pattern : new Pattern[]{PRICE_AND_SHIPPING, PRICE_THEN_SHIPPING}) {
            var match = pattern.matcher(title);
            if (match.find()) {
                try {
                    if (Long.parseLong(match.group(1).replace(",", "")) == price) {
                        return title.substring(0, match.start()).trim();
                    }
                } catch (NumberFormatException ignored) {
                    // 비정상적으로 큰 숫자는 삭제하지 않는다.
                }
            }
        }
        return title;
    }

    private static String normalizeUnicode(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }
}
