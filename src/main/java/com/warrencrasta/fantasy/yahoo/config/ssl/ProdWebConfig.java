package com.warrencrasta.fantasy.yahoo.config.ssl;

import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
public class ProdWebConfig {

    @Bean
    public WebServerFactoryCustomizer<AbstractServletWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            factory.setUseForwardHeaders(true);
        };
    }
}
