package com.authoritypro.platform.runtime.infrastructure.http;

import java.util.List;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class HttpSecurityConfiguration {

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = new CookieCsrfTokenRepository();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).sameSite("Strict"));
        return repository;
    }

    @Bean
    SecurityFilterChain closedHttpBoundary(HttpSecurity http, HttpProblems problems,
            CookieCsrfTokenRepository repository) throws Exception {
        // Match the original URI, not a decoded/normalized MVC pattern or a HEAD alias.
        RequestMatcher exactProbeGet = request -> request.getDispatcherType() == DispatcherType.REQUEST
                && "GET".equals(request.getMethod())
                && request.getContextPath().isEmpty()
                && ("/actuator/health/liveness".equals(request.getRequestURI())
                    || "/actuator/health/readiness".equals(request.getRequestURI()));

        http.authorizeHttpRequests(authorize -> authorize
                    .requestMatchers(exactProbeGet).permitAll()
                    .anyRequest().denyAll())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .servletApi(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .securityContext(context -> context
                    .securityContextRepository(new NullSecurityContextRepository())
                    .requireExplicitSave(true))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.csrfTokenRepository(repository)
                    .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler()))
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(problems)
                    .accessDeniedHandler(problems))
                .addFilterBefore(new HttpProblemBoundaryFilter(problems), DisableEncodeUrlFilter.class);
        return http.build();
    }

    @Bean
    WebSecurityCustomizer closedFirewall(HttpProblems problems) {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        // Syntactically valid standard methods reach denyAll (including TRACE/CONNECT).
        // Malformed methods and unsafe path encodings remain firewall bad requests.
        firewall.setAllowedHttpMethods(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE",
                "OPTIONS", "TRACE", "CONNECT"));
        return web -> web.httpFirewall(firewall).requestRejectedHandler(problems);
    }
}
