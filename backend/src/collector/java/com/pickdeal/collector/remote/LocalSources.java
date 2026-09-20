package com.pickdeal.collector.remote;

import com.github.zhkl0228.impersonator.ImpersonatorFactory;
import com.pickdeal.collector.ppomppu.PpomppuListParser;
import com.pickdeal.collector.quasarzone.*;
import com.pickdeal.collector.ruliweb.*;
import com.pickdeal.collector.support.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Proxy;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import okhttp3.*;
import org.jsoup.Jsoup;

/** 출처 URL은 고정하며 수신 API 토큰을 사이트 요청에 사용하지 않는다. */
public final class LocalSources implements AutoCloseable {
    private final OkHttpClient ppomppu;
    public LocalSources() {
        var profile = ImpersonatorFactory.macChrome();
        profile.setEchConfigProvider(null);
        ppomppu = OkHttpClientFactory.create(profile).newHttpClient().newBuilder()
                .proxy(Proxy.NO_PROXY).cookieJar(CookieJar.NO_COOKIES)
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .callTimeout(Duration.ofSeconds(20)).connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10)).build();
    }

    public RemoteCollectionRunner.Source source(String code) {
        if (!List.of("quasarzone", "ruliweb", "ppomppu").contains(code)) throw new IllegalArgumentException("Unknown source");
        return new RemoteCollectionRunner.Source() {
            public String code() { return code; }
            public boolean supportsDetails() { return !code.equals("ppomppu"); }
            public List<CollectedDeal> list(int page) {
                if (page < 1 || page > 3) throw new IllegalArgumentException("Invalid page");
                OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
                return switch (code) {
                    case "quasarzone" -> new QuasarzoneListParser().parse(fetch(
                            "https://quasarzone.com/bbs/qb_saleinfo" + (page == 1 ? "" : "?page=" + page), false))
                            .stream().map(i -> new CollectedDeal(i.externalId(),
                                    "https://quasarzone.com/bbs/qb_saleinfo/views/" + i.externalId(), i.storeName(), i.title(),
                                    i.price(), CategoryNormalizer.normalize(i.category()), i.commentCount(), i.thumbnailUrl(),
                                    i.ended(), new QuasarzonePostedAtResolver().resolve(i.postedAtText(), now))).toList();
                    case "ruliweb" -> new RuliwebListParser().parse(fetch(
                            "https://bbs.ruliweb.com/market/board/1020" + (page == 1 ? "" : "?page=" + page), false))
                            .stream().map(i -> new CollectedDeal(i.externalId(),
                                    "https://bbs.ruliweb.com/market/board/1020/read/" + i.externalId(), i.storeName(), i.title(),
                                    i.price(), CategoryNormalizer.normalize(i.category()), i.commentCount(), null,
                                    i.ended(), new RuliwebPostedAtResolver().resolve(i.postedAtText(), now))).toList();
                    default -> new PpomppuListParser().parse(fetch(
                            "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu" + (page == 1 ? "" : "&page=" + page), true))
                            .stream().map(i -> new CollectedDeal(i.externalId(), i.url(), i.storeName(), i.title(), i.price(),
                                    CategoryNormalizer.normalize(i.category()), i.commentCount(), i.thumbnailUrl(),
                                    null, ppomppuTime(i.postedAtText()), null)).toList();
                };
            }
            public CollectedDeal detail(CollectedDeal deal) {
                if (!supportsDetails()) return deal;
                // 임의 링크가 아니라 위에서 출처별 고정 경로로 구성한 원문 URL이다.
                String html = fetch(deal.url(), false);
                CollectedProductInfo info = code.equals("quasarzone")
                        ? new QuasarzoneDetailParser().parse(html) : new RuliwebDetailParser().parse(html);
                return deal.withProductInfo(info.shopName(), info.productUrl());
            }
        };
    }

    static OffsetDateTime ppomppuTime(String value) {
        if (value == null) return null;
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("uu.MM.dd HH:mm:ss")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT)).atOffset(ZoneOffset.ofHours(9));
        } catch (DateTimeException ignored) { return null; }
    }

    private String fetch(String url, boolean useImpersonator) {
        try {
            if (!useImpersonator) {
                var response = Jsoup.connect(url).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
                        .header("Accept-Language", "ko-KR,ko;q=0.9").timeout(10000).maxBodySize(2 * 1024 * 1024 + 1)
                        .followRedirects(false).execute();
                if (response.statusCode() != 200 || "challenge".equalsIgnoreCase(response.header("cf-mitigated"))
                        || response.bodyAsBytes().length > 2 * 1024 * 1024) throw new IOException("Source rejected");
                return response.parse().outerHtml();
            }
            try (var response = ppomppu.newCall(new Request.Builder().url(url).get().build()).execute()) {
                if (response.code() != 200 || "challenge".equalsIgnoreCase(response.header("cf-mitigated"))) throw new IOException("Source rejected");
                try (var body = response.peekBody(2 * 1024 * 1024 + 1)) {
                    byte[] bytes = body.bytes();
                    if (bytes.length > 2 * 1024 * 1024) throw new IOException("Source body limit");
                    return Jsoup.parse(new ByteArrayInputStream(bytes), null, url).outerHtml();
                }
            }
        } catch (IOException e) { throw new UncheckedIOException("Source request failed", e); }
    }
    @Override public void close() {
        ppomppu.dispatcher().executorService().shutdownNow();
        ppomppu.connectionPool().evictAll();
    }
}
