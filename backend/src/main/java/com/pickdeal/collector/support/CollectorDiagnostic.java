package com.pickdeal.collector.support;

import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

/** 고정된 뽐뿌 목록에 한 번 요청한다. Spring, DB, 스케줄러는 시작하지 않는다. */
public final class CollectorDiagnostic {
    private static final String URL = "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu";

    private CollectorDiagnostic() {}

    public static void main(String[] args) {
        System.exit(run(args, new HtmlFetcher(), System.out));
    }

    static int run(String[] args, HtmlFetcher fetcher, PrintStream out) {
        if (args.length != 0) {
            out.println("Arguments are not supported. Target is the fixed ppomppu list URL.");
            return 2;
        }
        out.println("target=" + URL);
        out.println("mode=single-fetch; timeout=20s; spring=false; database=false");
        long start = System.nanoTime();
        try {
            String html = fetcher.fetch(URL, Duration.ofSeconds(20));
            var document = Jsoup.parse(html, URL);
            long links = document.select("a[href]").stream()
                    .map(a -> a.absUrl("href"))
                    .filter(href -> href.startsWith("https://www.ppomppu.co.kr/zboard/view.php?"))
                    .filter(href -> href.matches(".*[?&]id=ppomppu(?:&.*|$)"))
                    .filter(href -> href.matches(".*[?&]no=[0-9]+(?:&.*|$)"))
                    .distinct().count();
            out.println("fetch=success");
            out.println("htmlChars=" + html.length());
            out.println("candidatePostLinks=" + links);
            out.println(links > 0 ? "result=LIST_CANDIDATE; manual validation still required"
                    : "result=UNVERIFIED; no matching post links (may be a security page or changed markup)");
            return links > 0 ? 0 : 3;
        } catch (UncheckedIOException e) {
            Throwable cause = e.getCause();
            out.println("fetch=failed");
            out.println("errorType=" + cause.getClass().getSimpleName());
            if (cause instanceof HttpStatusException status) {
                out.println("httpStatus=" + status.getStatusCode());
            }
            // 응답 본문, 쿠키, 예외 메시지는 토큰/개인정보를 포함할 수 있어 출력하지 않는다.
            return 1;
        } finally {
            out.println("elapsedMs=" + (System.nanoTime() - start) / 1_000_000);
        }
    }
}
