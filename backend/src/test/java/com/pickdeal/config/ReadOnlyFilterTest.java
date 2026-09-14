package com.pickdeal.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class ReadOnlyFilterTest {
    private final ReadOnlyFilter filter = new ReadOnlyFilter(new ObjectMapper());

    @Test
    void enabledOnlyWhenConfigured() {
        var runner = new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(ReadOnlyFilter.class);
        runner.run(context -> assertThat(context).doesNotHaveBean(ReadOnlyFilter.class));
        runner.withPropertyValues("pickdeal.read-only=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReadOnlyFilter.class));
        runner.withPropertyValues("pickdeal.read-only=true")
                .run(context -> assertThat(context).hasSingleBean(ReadOnlyFilter.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PATCH", "DELETE", "PUT", "TRACE"})
    void rejectsWritesBeforeController(String method) throws Exception {
        for (String path : new String[]{"/api/v1/keywords", "/api/v1/keywords/1",
                "/api/v1/sources/1/visibility", "/api/v1/internal/deals", "/other"}) {
            var request = new MockHttpServletRequest(method, path);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                throw new AssertionError("쓰기 요청은 Controller에 도달하면 안 된다");
            });
            assertThat(response.getStatus()).isEqualTo(403);
            assertThat(response.getContentAsString()).contains("\"code\":\"READ_ONLY\"");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void allowsReadAndPreflight(String method) throws Exception {
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(method, "/api/v1/deals"), response,
                (req, res) -> res.setContentType("passed"));
        assertThat(response.getContentType()).isEqualTo("passed");
    }
}
