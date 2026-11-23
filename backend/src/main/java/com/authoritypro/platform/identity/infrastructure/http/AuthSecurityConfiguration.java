package com.authoritypro.platform.identity.infrastructure.http;

import com.authoritypro.platform.identity.application.AuthenticationService;
import com.authoritypro.platform.identity.application.CredentialReader;
import com.authoritypro.platform.identity.domain.LoginThrottle;
import com.authoritypro.platform.identity.domain.PasswordVerifier;
import com.authoritypro.platform.identity.infrastructure.persistence.JdbcIdentityGateway;
import com.authoritypro.platform.identity.infrastructure.security.SpringPasswordVerifier;
import java.time.Clock;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import tools.jackson.databind.json.JsonMapper;

/**
 * The authenticated surface only exists when a runtime datasource is installed. Without one the
 * closed L02 boundary keeps denying every non-probe route.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class AuthSecurityConfiguration {

    @Bean
    AuthProblems authProblems(JsonMapper mapper) {
        return new AuthProblems(mapper);
    }

    @Bean
    HttpSessionSecurityContextRepository authSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    Clock identityClock() {
        return Clock.systemUTC();
    }

    @Bean
    LoginThrottle loginThrottle() {
        return new LoginThrottle();
    }

    @Bean
    PasswordVerifier passwordVerifier() {
        return new SpringPasswordVerifier();
    }

    @Bean
    JdbcIdentityGateway identityGateway(DataSource dataSource) {
        return new JdbcIdentityGateway(dataSource);
    }

    @Bean
    AuthenticationService authenticationService(CredentialReader credentials, PasswordVerifier verifier,
            LoginThrottle throttle, Clock identityClock) {
        return new AuthenticationService(credentials, verifier, throttle, identityClock);
    }

    @Bean
    @Order(1)
    SecurityFilterChain authenticatedHttpBoundary(HttpSecurity http, AuthProblems authProblems,
            CookieCsrfTokenRepository repository, HttpSessionSecurityContextRepository contextRepository)
            throws Exception {
        http.securityMatcher("/api/v1/auth/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .anyRequest().authenticated())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .servletApi(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .securityContext(context -> context.securityContextRepository(contextRepository)
                        .requireExplicitSave(true))
                .csrf(csrf -> csrf.csrfTokenRepository(repository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authProblems)
                        .accessDeniedHandler(authProblems));
        return http.build();
    }
}
