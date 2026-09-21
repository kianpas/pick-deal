package com.pickdeal.collector.dogdrip;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;

/** 인수 없으면 네트워크 목록 1회, 파일 인수면 오프라인. Spring/DB/상세 요청 없음. */
public final class DogdripDiagnostic {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Expected zero or one HTML path");
        String html = args.length == 1 ? Files.readString(Path.of(args[0]))
                : new DogdripClient(new DogdripCollectorProperties(false, Duration.ofSeconds(10))).fetchListHtml();
        var deals = new DogdripListParser().parse(html, OffsetDateTime.now());
        System.out.printf("source=dogdrip; items=%d; price=%d; thumbnails=%d; database=false; details=0%n",
                deals.size(), deals.stream().filter(d -> d.price() != null).count(),
                deals.stream().filter(d -> d.thumbnailUrl() != null).count());
    }
}
