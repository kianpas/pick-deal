package com.pickdeal.diagnostic;

import com.github.zhkl0228.impersonator.ImpersonatorFactory;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Proxy;
import java.time.Duration;
import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClientFactory;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;

/** 독립 진단 전용. 운영 앱과 공유하는 의존성/컨텍스트가 없다. */
public final class ImpersonatorDiagnostic {
    static final String TARGET = "https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu";
    static final int MAX_BODY = 2 * 1024 * 1024;

    public static void main(String[] args) {
        if (args.length != 0) {
            System.out.println("result=INVALID_ARGUMENTS; target is fixed; no request sent");
            System.exit(2);
        }
        int result;
        OkHttpClient client = null;
        long start = System.nanoTime();
        try {
            client = newClient();
            System.out.println("target=" + TARGET);
            System.out.println("client=impersonator-okhttp/1.10.2; profile=macChrome");
            System.out.println("mode=one-shot; redirects=false; retries=false; database=false");
            result = probe(client, TARGET, System.out);
        } catch (IOException | RuntimeException e) {
            System.out.println("result=FAILED; errorType=" + e.getClass().getSimpleName());
            result = 1;
        } finally {
            if (client != null) {
                client.dispatcher().executorService().shutdownNow();
                client.connectionPool().evictAll();
            }
            System.out.println("elapsedMs=" + (System.nanoTime() - start) / 1_000_000);
        }
        System.exit(result);
    }

    static OkHttpClient newClient() {
        var profile = ImpersonatorFactory.macChrome();
        // 기본 외부 DoH 조회를 하지 않는다. 브라우저 프로필의 GREASE ECH만 유지.
        profile.setEchConfigProvider(null);
        return OkHttpClientFactory.create(profile).newHttpClient().newBuilder()
                .proxy(Proxy.NO_PROXY).cookieJar(CookieJar.NO_COOKIES)
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .callTimeout(Duration.ofSeconds(20)).connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(10)).build();
    }

    static int probe(OkHttpClient client, String target, PrintStream out) throws IOException {
        Request request = new Request.Builder().url(target).get().build();
        try (Response response = client.newCall(request).execute()) {
            out.println("httpStatus=" + response.code());
            out.println("protocol=" + response.protocol());
            if (!response.isSuccessful()) {
                out.println("result=HTTP_REJECTED; no retry");
                return 1;
            }
            if ("challenge".equalsIgnoreCase(response.header("cf-mitigated"))) {
                out.println("result=CHALLENGE; stopped");
                return 3;
            }
            // 응답 본문 전체를 메모리에 읽거나 쿠키/제목을 로그로 남기지 않는다.
            try (var body = response.peekBody(MAX_BODY + 1L)) {
                if (body.contentLength() > MAX_BODY) {
                    out.println("result=BODY_LIMIT; stopped");
                    return 3;
                }
                return inspect(body.string(), target, out);
            }
        }
    }

    static int inspect(String html, String baseUrl, PrintStream out) {
        var doc = Jsoup.parse(html, baseUrl);
        long links = doc.select("a[href]").stream().map(a -> a.absUrl("href"))
                .filter(url -> url.startsWith("https://www.ppomppu.co.kr/zboard/view.php?"))
                .filter(url -> url.matches(".*[?&]id=ppomppu(?:&.*|$)"))
                .filter(url -> url.matches(".*[?&]no=[0-9]+(?:&.*|$)"))
                .distinct().count();
        out.println("candidatePostLinks=" + links);
        out.println(links > 0 ? "result=LIST_CANDIDATE; not proof of freshness or collection permission"
                : "result=UNVERIFIED; no post links");
        return links > 0 ? 0 : 3;
    }
}
