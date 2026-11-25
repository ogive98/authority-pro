package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.AuditAccessPolicy;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcAuditAccessPolicy;
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
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

/** Access owns the authorization boundary of the tenant audit read surface. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class AuditSecurityConfiguration {

    @Bean
    AuditAccessPolicy auditAccessPolicy(DataSource dataSource) {
        return new JdbcAuditAccessPolicy(dataSource);
    }

    @Bean
    AuditAuthorizationFilter auditAuthorizationFilter(AuditAccessPolicy policy, AccessProblems problems) {
        return new AuditAuthorizationFilter(policy, problems);
    }

    @Bean
    @Order(4)
    SecurityFilterChain auditHttpBoundary(HttpSecurity http, AccessProblems problems,
            AuditAuthorizationFilter authorizationFilter) throws Exception {
        HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
        http.securityMatcher("/api/v1/audit/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/audit/tenant-records").authenticated()
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
                        .accessDeniedHandler(problems))
                .addFilterAfter(authorizationFilter, SecurityContextHolderFilter.class);
        return http.build();
    }
}
