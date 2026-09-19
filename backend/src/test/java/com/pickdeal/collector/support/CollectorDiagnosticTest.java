package com.pickdeal.collector.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import org.jsoup.HttpStatusException;
import org.junit.jupiter.api.Test;

class CollectorDiagnosticTest {
    private final HtmlFetcher fetcher = mock(HtmlFetcher.class);
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(output);

    @Test
    void fetchesOnceAndCountsDistinctPostLinksWithoutPrintingContent() {
        when(fetcher.fetch(any(), any())).thenReturn("""
                <a href="view.php?id=ppomppu&no=123">private title</a>
                <a href="view.php?id=ppomppu&no=123">duplicate</a>
                <a href="view.php?id=notice&no=123">notice</a>
                """);
        assertThat(CollectorDiagnostic.run(new String[0], fetcher, out)).isZero();
        assertThat(output.toString()).contains("candidatePostLinks=1").doesNotContain("private title");
        verify(fetcher, times(1)).fetch(eq("https://www.ppomppu.co.kr/zboard/zboard.php?id=ppomppu"),
                eq(Duration.ofSeconds(20)));
        verifyNoMoreInteractions(fetcher);
    }

    @Test
    void successfulFetchWithoutPostLinksIsNotReportedAsVerified() {
        when(fetcher.fetch(any(), any())).thenReturn("<title>Just a moment</title>");
        assertThat(CollectorDiagnostic.run(new String[0], fetcher, out)).isEqualTo(3);
        assertThat(output.toString()).contains("result=UNVERIFIED");
    }

    @Test
    void reports403WithoutRetryOrLeakingExceptionMessage() {
        when(fetcher.fetch(any(), any())).thenThrow(new UncheckedIOException(
                new HttpStatusException("secret", 403, "https://example.com/?token=secret")));
        assertThat(CollectorDiagnostic.run(new String[0], fetcher, out)).isEqualTo(1);
        assertThat(output.toString()).contains("httpStatus=403").doesNotContain("secret");
        verify(fetcher, times(1)).fetch(any(), any());
    }

    @Test
    void reportsTimeout() {
        when(fetcher.fetch(any(), any())).thenThrow(new UncheckedIOException(new SocketTimeoutException()));
        assertThat(CollectorDiagnostic.run(new String[0], fetcher, out)).isEqualTo(1);
        assertThat(output.toString()).contains("errorType=SocketTimeoutException");
    }

    @Test
    void rejectsArgumentsWithoutNetworkRequest() {
        assertThat(CollectorDiagnostic.run(new String[]{"https://example.com"}, fetcher, out)).isEqualTo(2);
        verifyNoInteractions(fetcher);
    }
}
