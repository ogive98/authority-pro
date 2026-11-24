package com.authoritypro.platform.access.infrastructure.http;

import com.authoritypro.platform.access.application.InventoryAccessPolicy;
import com.authoritypro.platform.access.infrastructure.persistence.JdbcInventoryAccessPolicy;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Access owns the authorization boundary of the protected inventory; the runtime serves the view
 * behind it. No cross-root Java edge is introduced.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class InventorySecurityConfiguration {

    @Bean
    InventoryProblems inventoryProblems(JsonMapper mapper) {
        return new InventoryProblems(mapper);
    }

    @Bean
    InventoryAccessPolicy inventoryAccessPolicy(DataSource dataSource) {
        return new JdbcInventoryAccessPolicy(dataSource);
    }

    @Bean
    HttpSessionSecurityContextRepository inventorySecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    InventoryAuthorizationFilter inventoryAuthorizationFilter(InventoryAccessPolicy policy,
            InventoryProblems problems) {
        return new InventoryAuthorizationFilter(policy, problems);
    }

    @Bean
    @Order(2)
    SecurityFilterChain inventoryHttpBoundary(HttpSecurity http, InventoryProblems problems,
            HttpSessionSecurityContextRepository contextRepository,
            InventoryAuthorizationFilter authorizationFilter) throws Exception {
        http.securityMatcher("/api/v1/platform/inventory")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/platform/inventory").authenticated()
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
