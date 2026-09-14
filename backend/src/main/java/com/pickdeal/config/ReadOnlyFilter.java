package com.pickdeal.config;

import com.pickdeal.common.error.ErrorCode;
import com.pickdeal.common.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** 공개 조회 서버에서는 경로나 요청 본문에 관계없이 HTTP 쓰기를 거부한다. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "pickdeal.read-only", havingValue = "true")
public class ReadOnlyFilter extends OncePerRequestFilter {
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final ObjectMapper mapper;

    public ReadOnlyFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!READ_METHODS.contains(request.getMethod())) {
            ErrorCode error = ErrorCode.READ_ONLY;
            response.setStatus(error.getStatus().value());
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(), ApiResponse.error(error.getCode(),
                    "공개 조회 환경에서는 데이터를 변경할 수 없습니다."));
            return;
        }
        chain.doFilter(request, response);
    }
}
