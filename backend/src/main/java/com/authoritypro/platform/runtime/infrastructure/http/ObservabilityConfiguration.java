package com.authoritypro.platform.runtime.infrastructure.http;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Registers correlation before the closed security chain so every response carries it. */
@Configuration(proxyBeanMethods = false)
public class ObservabilityConfiguration {

    @Bean
    FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter(MeterRegistry registry) {
        FilterRegistrationBean<RequestCorrelationFilter> registration =
                new FilterRegistrationBean<>(new RequestCorrelationFilter(registry));
        registration.setName("requestCorrelationFilter");
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
