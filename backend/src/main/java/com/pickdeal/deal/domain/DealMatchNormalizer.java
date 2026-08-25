package com.pickdeal.deal.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;

/** 교차 출처 후보 탐색에 사용할 보수적인 제목·판매몰 정규화 규칙. */
public final class DealMatchNormalizer {

    private DealMatchNormalizer() {
    }

    /** 판매몰 말머리만 제거하고 모델·용량·수량을 포함한 나머지 글자는 모두 보존한다. */
    public static String normalizeTitle(String title, String shopName) {
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

        String result = normalizedTitle
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return result.isBlank() ? null : result;
    }

    public static String titleHash(String title, String shopName) {
        String normalizedTitle = normalizeTitle(title, shopName);
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
        return result.isBlank() ? null : result;
    }

    private static String normalizeUnicode(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }
}
