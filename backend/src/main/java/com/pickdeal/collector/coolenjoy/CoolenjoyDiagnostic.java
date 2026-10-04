package com.pickdeal.collector.coolenjoy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** 기본 목록 1회, --with-details는 최대 3건 보강. 파일 인수는 오프라인. Spring·DB 없음. */
public final class CoolenjoyDiagnostic {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Expected HTML file path or --with-details");
        boolean withDetails = args.length == 1 && args[0].equals("--with-details");
        var client = new CoolenjoyClient(new CoolenjoyCollectorProperties(false, Duration.ofSeconds(10), 3));
        String html = args.length == 1 && !withDetails ? Files.readString(Path.of(args[0])) : client.fetchListHtml();
        var deals = new CoolenjoyListParser().parse(html, java.time.OffsetDateTime.now());
        int details = 0;
        if (withDetails) {
            var enriched = new java.util.ArrayList<>(deals);
            var parser = new CoolenjoyDetailParser();
            for (int i = 0; i < Math.min(3, enriched.size()); i++) {
                details++;
                var deal = enriched.get(i);
                enriched.set(i, parser.enrich(client.fetchDetailHtml(deal.url()), deal));
            }
            deals = enriched;
        }
        System.out.printf("source=coolenjoy; items=%d; price=%d; thumbnails=%d; productUrl=%d; postedAt=%d; database=false; details=%d%n",
                deals.size(), deals.stream().filter(d -> d.price() != null).count(),
                deals.stream().filter(d -> d.thumbnailUrl() != null).count(),
                deals.stream().filter(d -> d.productUrl() != null).count(),
                deals.stream().filter(d -> d.postedAt() != null).count(), details);
    }
}
