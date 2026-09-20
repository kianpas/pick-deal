package com.pickdeal.collector.remote;

import java.util.Arrays;
import java.util.List;

/** SpringApplication을 호출하지 않는다. HTTP 서버·DataSource·JPA·Flyway 모두 기동하지 않는다. */
public final class LocalCollectorMain {
    public static void main(String[] args) {
        int result;
        try { result = run(args); }
        catch (Exception failure) {
            // 환경변수/토큰/응답 본문/중첩 HTTP 예외를 로그에 노출하지 않는다.
            System.err.println("collectorStopped=" + failure.getClass().getSimpleName() + "; check configuration and receiver");
            result = 1;
        }
        System.exit(result);
    }
    private static int run(String[] args) throws InterruptedException {
        if (args.length != 0) throw new IllegalArgumentException("Arguments not supported; use environment variables");
        String codes = required("COLLECTOR_SOURCES");
        List<String> sources = Arrays.stream(codes.split(",", -1)).map(String::trim).distinct().toList();
        if (sources.stream().anyMatch(s -> !List.of("quasarzone", "ruliweb", "ppomppu").contains(s))) throw new IllegalArgumentException("Unknown source");
        String onceValue = System.getenv().getOrDefault("COLLECTOR_RUN_ONCE", "true");
        if (!List.of("true", "false").contains(onceValue)) throw new IllegalArgumentException("Invalid run mode");
        boolean once = Boolean.parseBoolean(onceValue);
        int pages = Integer.parseInt(System.getenv().getOrDefault("COLLECTOR_BOOTSTRAP_MAX_PAGES", "1"));
        int details = Integer.parseInt(System.getenv().getOrDefault("COLLECTOR_MAX_DETAIL_REQUESTS", "3"));
        try (var api = new RemoteApiClient(required("COLLECTOR_RECEIVER_URL"), required("COLLECTOR_INGRESS_TOKEN"));
                var clients = new LocalSources()) {
            var runner = new RemoteCollectionRunner(api, pages, details);
            System.out.println("mode=" + (once ? "once" : "continuous") + "; database=false; webServer=false; sources=" + String.join(",", sources));
            do {
                boolean failed = false;
                for (String source : sources) {
                    try { runner.run(clients.source(source)); }
                    catch (RemoteApiClient.Failure error) {
                        System.err.println("receiverFailed=[" + source + "]; " + error.getMessage());
                        if (error.permanent()) return 1;
                        failed = true;
                    } catch (RuntimeException error) {
                        System.err.println("sourceFailed=[" + source + "]; errorType=" + error.getClass().getSimpleName());
                        failed = true;
                    }
                }
                if (once) return failed ? 1 : 0;
                Thread.sleep(java.time.Duration.ofMinutes(20).toMillis());
            } while (!Thread.currentThread().isInterrupted());
        }
        return 0;
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }
}
