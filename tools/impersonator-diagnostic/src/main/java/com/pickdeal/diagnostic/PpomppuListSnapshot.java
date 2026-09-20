package com.pickdeal.diagnostic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayInputStream;
import okhttp3.Request;
import org.jsoup.Jsoup;

/** 목록 파서 개발용 단일 요청. 원본은 git에서 제외된 build 아래에만 저장한다. */
public final class PpomppuListSnapshot {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("No arguments accepted");
        var client = ImpersonatorDiagnostic.newClient();
        try (var response = client.newCall(new Request.Builder()
                .url(ImpersonatorDiagnostic.TARGET).get().build()).execute()) {
            System.out.println("httpStatus=" + response.code());
            if (!response.isSuccessful() || "challenge".equalsIgnoreCase(response.header("cf-mitigated"))) {
                throw new IllegalStateException("Request rejected; no retry");
            }
            try (var body = response.peekBody(ImpersonatorDiagnostic.MAX_BODY + 1L)) {
                byte[] bytes = body.bytes();
                if (bytes.length > ImpersonatorDiagnostic.MAX_BODY) throw new IllegalStateException("Body limit");
                var document = Jsoup.parse(new ByteArrayInputStream(bytes), null, ImpersonatorDiagnostic.TARGET);
                if (ImpersonatorDiagnostic.inspect(document.outerHtml(), ImpersonatorDiagnostic.TARGET, System.out) != 0) {
                    throw new IllegalStateException("No list candidates; snapshot not saved");
                }
                Path output = Path.of("build/ppomppu-list.raw.html");
                Files.createDirectories(output.getParent());
                Files.write(output, bytes);
                System.out.println("saved=" + output + "; bytes=" + bytes.length + "; database=false");
            }
        } finally {
            client.dispatcher().executorService().shutdownNow();
            client.connectionPool().evictAll();
        }
    }
}
