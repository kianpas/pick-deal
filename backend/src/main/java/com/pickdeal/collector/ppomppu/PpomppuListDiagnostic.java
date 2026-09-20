package com.pickdeal.collector.ppomppu;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jsoup.Jsoup;

/** 확보한 로컬 HTML만 읽는 오프라인 확인 도구. Spring/DB/네트워크를 사용하지 않는다. */
public final class PpomppuListDiagnostic {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: PpomppuListDiagnostic <html-file>");
        Path input = Path.of(args[0]);
        if (Files.size(input) > 2 * 1024 * 1024) throw new IllegalArgumentException("HTML exceeds 2MiB");
        try (var stream = Files.newInputStream(input)) {
            // null charset: meta charset(euc-kr 등)를 Jsoup이 감지한다.
            var document = Jsoup.parse(stream, null, PpomppuListParser.BASE_URL);
            var items = new PpomppuListParser().parse(document.outerHtml());
            System.out.println("mode=offline; database=false; network=false");
            System.out.printf("parsed=%d price=%d shop=%d thumbnail=%d comments=%d%n", items.size(),
                    items.stream().filter(i -> i.price() != null).count(),
                    items.stream().filter(i -> i.storeName() != null).count(),
                    items.stream().filter(i -> i.thumbnailUrl() != null).count(),
                    items.stream().filter(i -> i.commentCount() != null).count());
            items.stream().limit(3).forEach(System.out::println);
        }
    }
}
