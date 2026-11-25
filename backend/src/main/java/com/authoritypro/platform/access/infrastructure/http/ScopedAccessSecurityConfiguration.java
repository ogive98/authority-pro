package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.ScopedAccessReader;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcScopedAccessReader;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/** Bootstrap surface: authenticated, read-only and bound to a server-validated selector. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class ScopedAccessSecurityConfiguration {

    @Bean
    ScopedAccessReader scopedAccessReader(DataSource dataSource) {
        return new JdbcScopedAccessReader(dataSource);
    }

    @Bean
    @Order(3)
    SecurityFilterChain scopedAccessHttpBoundary(HttpSecurity http, AccessProblems problems) throws Exception {
        HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
        http.securityMatcher("/api/v1/access/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/access/bootstrap").authenticated()
                        .anyRequest().denyAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .servletApi(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .securityContext(context -> context.securityContextRepository(contextRepository)
                        .requireExplicitSave(true))
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems));
        return http.build();
    }
}
