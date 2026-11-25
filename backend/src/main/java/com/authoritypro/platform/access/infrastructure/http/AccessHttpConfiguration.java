package com.authoritypro.platform.access.infrastructure.http;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Shared HTTP error envelope for the scoped access and audit chains. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class AccessHttpConfiguration {

    @Bean
    AccessProblems accessProblems(JsonMapper mapper) {
        return new AccessProblems(mapper);
    }
}
