package com.pickdeal.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class CollectorIngressFilterTest {
    private static final String TOKEN = "test-only-collector-token-0123456789";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void disabledByDefaultEvenWithCorrectToken() throws Exception {
        var request = request(CollectorIngressFilter.ROOT);
        var response = new MockHttpServletResponse();
        new CollectorIngressFilter(false, TOKEN, mapper).doFilter(request, response,
                (req, res) -> { throw new AssertionError("must not pass"); });
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @ParameterizedTest @ValueSource(strings = {"", "short", "contains spaces in token 01234567890123456789"})
    void refusesInsecureConfiguration(String token) {
        assertThatThrownBy(() -> new CollectorIngressFilter(true, token, mapper))
                .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining(token.isEmpty() ? "secret" : token);
    }

    @Test void rejectsDuplicateAuthorizationHeaders() throws Exception {
        var request = request(CollectorIngressFilter.ROOT);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        var response = new MockHttpServletResponse();
        new CollectorIngressFilter(true, TOKEN, mapper).doFilter(request, response,
                (req, res) -> { throw new AssertionError(); });
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test void capsChunkedBodyEvenWithoutContentLength() throws Exception {
        var request = new MockHttpServletRequest("POST", CollectorIngressFilter.ROOT) {
            @Override public long getContentLengthLong() { return -1; }
        };
        request.addHeader("Authorization", "Bearer " + TOKEN);
        request.setContent(new byte[1024 * 1024 + 1]);
        var response = new MockHttpServletResponse();
        new CollectorIngressFilter(true, TOKEN, mapper).doFilter(request, response,
                (req, res) -> { throw new AssertionError(); });
        assertThat(response.getStatus()).isEqualTo(413);
    }

    @ParameterizedTest @ValueSource(strings = {"/api/v1/internal/collected-deals", "/api/v1/internal/collected-deals/known-external-ids"})
    void onlyAuthenticatedExactPathsPassReadOnlyFilter(String path) throws Exception {
        var request = request(path);
        var response = new MockHttpServletResponse();
        new CollectorIngressFilter(true, TOKEN, mapper).doFilter(request, response,
                (req, res) -> new ReadOnlyFilter(mapper).doFilter(req, res, (r, s) -> s.setContentType("passed")));
        assertThat(response.getContentType()).isEqualTo("passed");
        assertThat(CollectorIngressFilter.isAuthenticated(request)).isFalse();
    }

    @ParameterizedTest @ValueSource(strings = {"/api/v1/keywords", "/api/v1/internal/deals", "/api/v1/sources/1/visibility"})
    void tokenNeverUnlocksOtherWrites(String path) throws Exception {
        var response = new MockHttpServletResponse();
        new CollectorIngressFilter(true, TOKEN, mapper).doFilter(request(path), response,
                (req, res) -> new ReadOnlyFilter(mapper).doFilter(req, res, (r, s) -> { throw new AssertionError(); }));
        assertThat(response.getStatus()).isEqualTo(403);
    }

    private MockHttpServletRequest request(String path) {
        var request = new MockHttpServletRequest("POST", path);
        request.addHeader("Authorization", "Bearer " + TOKEN);
        return request;
    }
}
