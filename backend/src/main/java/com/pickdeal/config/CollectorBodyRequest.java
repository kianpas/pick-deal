package com.pickdeal.config;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 인증 후 최대 1MiB만 읽은 JSON을 동기 MVC에 전달한다(청크 요청도 동일 상한). */
final class CollectorBodyRequest extends HttpServletRequestWrapper {
    private final byte[] body;
    CollectorBodyRequest(HttpServletRequest request, byte[] body) {
        super(request);
        this.body = body;
    }
    @Override public int getContentLength() { return body.length; }
    @Override public long getContentLengthLong() { return body.length; }
    @Override public ServletInputStream getInputStream() {
        var input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public int read() { return input.read(); }
            @Override public int read(byte[] b, int off, int len) { return input.read(b, off, len); }
            @Override public boolean isFinished() { return input.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException("Synchronous JSON only"); }
        };
    }
    @Override public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
}
