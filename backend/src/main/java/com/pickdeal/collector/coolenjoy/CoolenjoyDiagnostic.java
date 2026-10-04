package com.pickdeal.collector.coolenjoy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** 인수 없으면 HTML 목록 요청 1회, 파일 인수면 오프라인. Spring·DB·상세 요청 없음. */
public final class CoolenjoyDiagnostic {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Expected zero or one HTML file path");
        String html = args.length == 1 ? Files.readString(Path.of(args[0]))
                : new CoolenjoyClient(new CoolenjoyCollectorProperties(false, Duration.ofSeconds(10))).fetchListHtml();
        var deals = new CoolenjoyListParser().parse(html, java.time.OffsetDateTime.now());
        System.out.printf("source=coolenjoy; items=%d; price=%d; thumbnails=%d; productUrl=%d; postedAt=%d; database=false; details=0%n",
                deals.size(), deals.stream().filter(d -> d.price() != null).count(),
                deals.stream().filter(d -> d.thumbnailUrl() != null).count(),
                deals.stream().filter(d -> d.productUrl() != null).count(),
                deals.stream().filter(d -> d.postedAt() != null).count());
    }
}
