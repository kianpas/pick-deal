package com.pickdeal.diagnostic;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImpersonatorDiagnosticTest {
    @Test void stopsOnChallengeAndOversizedBody() throws Exception {
        for (boolean challenge : new boolean[]{true, false}) {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = challenge ? new byte[0] : new byte[ImpersonatorDiagnostic.MAX_BODY + 1];
                if (challenge) exchange.getResponseHeaders().add("cf-mitigated", "challenge");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            var client = ImpersonatorDiagnostic.newClient();
            try {
                var bytes = new ByteArrayOutputStream();
                assertEquals(3, ImpersonatorDiagnostic.probe(client,
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/", new PrintStream(bytes)));
                assertTrue(bytes.toString().contains(challenge ? "result=CHALLENGE" : "result=BODY_LIMIT"));
            } finally {
                client.dispatcher().executorService().shutdownNow();
                client.connectionPool().evictAll();
                server.stop(0);
            }
        }
    }

    @Test void parsesCandidatesWithoutLeakingTitles() {
        var bytes = new ByteArrayOutputStream();
        int code = ImpersonatorDiagnostic.inspect("""
                <a href="view.php?id=ppomppu&no=123">SECRET</a>
                <a href="view.php?id=ppomppu&no=123">duplicate</a>
                <a href="view.php?id=notice&no=456">notice</a>
                """, ImpersonatorDiagnostic.TARGET, new PrintStream(bytes));
        assertEquals(0, code);
        assertTrue(bytes.toString().contains("candidatePostLinks=1"));
        assertFalse(bytes.toString().contains("SECRET"));
        assertEquals(3, ImpersonatorDiagnostic.inspect("<title>Forbidden</title>",
                ImpersonatorDiagnostic.TARGET, new PrintStream(bytes)));
    }

    @Test void rejects403AndRedirectWithoutRetryOrSecretOutput() throws Exception {
        for (int status : new int[]{403, 302}) {
            var hits = new AtomicInteger();
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                hits.incrementAndGet();
                exchange.getResponseHeaders().add("Location", "/redirected");
                exchange.getResponseHeaders().add("Set-Cookie", "secret=value");
                exchange.sendResponseHeaders(status, -1);
                exchange.close();
            });
            server.start();
            var client = ImpersonatorDiagnostic.newClient();
            try {
                var bytes = new ByteArrayOutputStream();
                assertEquals(1, ImpersonatorDiagnostic.probe(client,
                        "http://127.0.0.1:" + server.getAddress().getPort() + "/", new PrintStream(bytes)));
                assertEquals(1, hits.get());
                assertTrue(bytes.toString().contains("httpStatus=" + status));
                assertFalse(bytes.toString().contains("secret"));
                assertEquals(20_000, client.callTimeoutMillis());
                assertFalse(client.retryOnConnectionFailure());
            } finally {
                client.dispatcher().executorService().shutdownNow();
                client.connectionPool().evictAll();
                server.stop(0);
            }
        }
    }
}
