package com.pickdeal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/** 운영 지표는 관리 포트에서 허용한 엔드포인트만 노출하고, 공개 API 포트에는 actuator가 없다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "pickdeal.read-only=true")
// Spring Boot 테스트는 기본으로 지표 내보내기를 끈다. 운영과 같은 노출을 검증하려고 켠다.
@AutoConfigureMetrics
class ActuatorExposureTest {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int apiPort;

    @LocalManagementPort
    private int managementPort;

    @Test
    @DisplayName("관리 포트는 health·metrics·prometheus만 노출하고 health 상세를 숨긴다")
    void managementPortExposesOnlyAllowedEndpoints() throws Exception {
        assertThat(managementPort).isNotEqualTo(apiPort);

        HttpResponse<String> prometheus = get(managementPort, "/actuator/prometheus");
        assertThat(prometheus.statusCode()).isEqualTo(200);
        assertThat(prometheus.body())
                .contains("pickdeal_deals_stored")
                .contains("pickdeal_grouping_lock_wait_seconds")
                .contains("application=\"pick-deal-api\"");

        HttpResponse<String> health = get(managementPort, "/actuator/health");
        assertThat(health.statusCode()).isEqualTo(200);
        assertThat(health.body()).contains("\"status\":\"UP\"").doesNotContain("components");

        assertThat(get(managementPort, "/actuator/metrics/jvm.memory.used").statusCode()).isEqualTo(200);
        for (String hidden : new String[] {"/actuator/env", "/actuator/heapdump", "/actuator/loggers", "/actuator/beans"}) {
            assertThat(get(managementPort, hidden).statusCode()).as(hidden).isEqualTo(404);
        }
    }

    @Test
    @DisplayName("공개 API 포트에서는 actuator 경로를 제공하지 않고, 없는 경로는 500이 아니라 404로 응답한다")
    void apiPortHasNoActuator() throws Exception {
        assertThat(get(apiPort, "/actuator/prometheus").statusCode()).isEqualTo(404);
        assertThat(get(apiPort, "/actuator/health").statusCode()).isEqualTo(404);
        HttpResponse<String> unknown = get(apiPort, "/api/v1/no-such-path");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(unknown.body()).contains("NOT_FOUND");
    }

    @Test
    @DisplayName("API 응답 시간은 백분위 계산용 히스토그램으로 기록된다")
    void recordsApiLatencyHistogram() throws Exception {
        get(apiPort, "/api/v1/deals");
        String body = get(managementPort, "/actuator/prometheus").body();
        assertThat(body).contains("http_server_requests_seconds_bucket").contains("uri=\"/api/v1/deals\"");
    }

    private HttpResponse<String> get(int port, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
