package com.pickdeal.collector.support;

import java.net.URI;

/** 외부 링크로 노출 가능한 절대 HTTP(S) URL만 허용한다. */
public final class ExternalUrlSupport {

    private ExternalUrlSupport() {
    }

    public static String httpUrlOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String candidate = value.trim();
        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null) {
                return candidate;
            }
        } catch (IllegalArgumentException ignored) {
            // 파싱할 수 없는 외부 입력은 상품 링크로 인정하지 않는다.
        }
        return null;
    }
}
