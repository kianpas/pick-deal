package com.pickdeal.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class CorsConfigTest {
    @Configuration
    @EnableWebMvc
    static class TestWebConfig {
        @Bean ProbeController probeController() { return new ProbeController(); }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/probe") String probe() { return "ok"; }
    }

    private void verify(String origins, boolean localhost, Check check) throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(TestWebConfig.class);
            context.addBeanFactoryPostProcessor(factory -> factory.registerSingleton(
                    "corsConfigurer", new CorsConfig().corsConfigurer(origins, localhost)));
            context.refresh();
            check.run(MockMvcBuilders.webAppContextSetup(context).build());
        }
    }

    @FunctionalInterface interface Check { void run(MockMvc mvc) throws Exception; }

    @Test
    void productionAllowsConfiguredOriginAndJsonPreflightOnly() throws Exception {
        verify(" https://pick-deal.vercel.app, https://preview.example.com ", false, mvc -> {
            mvc.perform(options("/api/v1/probe")
                    .header("Origin", "https://pick-deal.vercel.app")
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "https://pick-deal.vercel.app"))
                    .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
            mvc.perform(get("/api/v1/probe").header("Origin", "https://preview.example.com"))
                    .andExpect(status().isOk());
            for (String origin : new String[]{"https://evil.example", "http://localhost:3000",
                    "https://pick-deal.vercel.app.evil.example"}) {
                mvc.perform(get("/api/v1/probe").header("Origin", origin))
                        .andExpect(status().isForbidden())
                        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
            }
        });
    }

    @Test
    void emptyProductionOriginsDenyCrossOriginButAllowServerRequests() throws Exception {
        verify("", false, mvc -> {
            mvc.perform(get("/api/v1/probe")).andExpect(status().isOk());
            mvc.perform(get("/api/v1/probe").header("Origin", "https://pick-deal.vercel.app"))
                    .andExpect(status().isForbidden());
        });
    }

    @Test
    void localDevelopmentKeepsVariablePorts() throws Exception {
        verify("", true, mvc -> mvc.perform(get("/api/v1/probe").header("Origin", "http://localhost:13000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:13000")));
    }
}
