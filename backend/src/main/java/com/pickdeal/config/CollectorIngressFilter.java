package com.pickdeal.config;

import com.pickdeal.common.error.ErrorCode;
import com.pickdeal.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** 기본 비활성. 인증은 JSON 역직렬화와 조회 전용 필터보다 먼저 수행한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CollectorIngressFilter extends OncePerRequestFilter {
    public static final String ROOT = "/api/v1/internal/collected-deals";
    private static final String AUTHENTICATED = CollectorIngressFilter.class.getName() + ".authenticated";
    private static final Set<String> PATHS = Set.of(ROOT, ROOT + "/known-external-ids");
    private static final int MAX_BODY = 1024 * 1024;
    private final boolean enabled;
    private final byte[] expected;
    private final ObjectMapper mapper;

    public CollectorIngressFilter(@Value("${pickdeal.collector.ingress.enabled:false}") boolean enabled,
            @Value("${pickdeal.collector.ingress.token:}") String token, ObjectMapper mapper) {
        if (enabled && !token.matches("[A-Za-z0-9_-]{32,256}")) {
            throw new IllegalArgumentException("Collector ingress requires a 32-256 character URL-safe token");
        }
        this.enabled = enabled;
        this.expected = ("Bearer " + token).getBytes(StandardCharsets.UTF_8);
        this.mapper = mapper;
    }

    private static String path(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) && PATHS.contains(path(request))
                && Boolean.TRUE.equals(request.getAttribute(AUTHENTICATED));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!path(request).startsWith(ROOT)) {
            chain.doFilter(request, response);
            return;
        }
        if (!enabled || !PATHS.contains(path(request))) {
            reject(response, ErrorCode.NOT_FOUND, "수집 수신 경로를 사용할 수 없습니다.");
            return;
        }
        var headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.size() != 1 || headers.get(0).length() > 263
                || !MessageDigest.isEqual(expected, headers.get(0).getBytes(StandardCharsets.UTF_8))) {
            response.setHeader("WWW-Authenticate", "Bearer");
            reject(response, ErrorCode.UNAUTHORIZED, "수집기 인증이 필요합니다.");
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            response.setHeader("Allow", "POST");
            reject(response, ErrorCode.METHOD_NOT_ALLOWED, "POST 요청만 허용합니다.");
            return;
        }
        if (request.getContentLengthLong() > MAX_BODY) {
            reject(response, ErrorCode.PAYLOAD_TOO_LARGE, "수집 배치는 1MiB 이하여야 합니다.");
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_BODY + 1);
        if (body.length > MAX_BODY) {
            reject(response, ErrorCode.PAYLOAD_TOO_LARGE, "수집 배치는 1MiB 이하여야 합니다.");
            return;
        }
        request.setAttribute(AUTHENTICATED, true);
        try { chain.doFilter(new CollectorBodyRequest(request, body), response); }
        finally { request.removeAttribute(AUTHENTICATED); }
    }

    private void reject(HttpServletResponse response, ErrorCode error, String message) throws IOException {
        response.setStatus(error.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(response.getWriter(), ApiResponse.error(error.getCode(), message));
    }
}
