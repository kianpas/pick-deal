package com.pickdeal.collector;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickdeal.collector.quasarzone.QuasarzoneCollectService;
import com.pickdeal.collector.ruliweb.RuliwebCollectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {
        "pickdeal.collector.sources.quasarzone.enabled=false",
        "pickdeal.collector.sources.ruliweb.enabled=false"
})
class CollectorEnabledConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("비활성화한 출처 수집기는 애플리케이션에 등록하지 않는다")
    void disabledCollectorsAreNotRegistered() {
        assertThat(context.getBeansOfType(QuasarzoneCollectService.class)).isEmpty();
        assertThat(context.getBeansOfType(RuliwebCollectService.class)).isEmpty();
    }
}
