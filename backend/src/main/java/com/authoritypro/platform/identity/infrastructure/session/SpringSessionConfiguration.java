package com.authoritypro.platform.identity.infrastructure.session;

import com.authoritypro.platform.identity.application.SessionLimiter;
import com.authoritypro.platform.identity.infrastructure.http.AuthProblems;
import com.authoritypro.platform.identity.infrastructure.http.SessionStoreFailureFilter;
import jakarta.servlet.Filter;
import java.time.Duration;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spring Session JDBC is wired explicitly, and only when the runtime datasource exists. The
 * migration creates the tables; the application never initializes schema.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class SpringSessionConfiguration {
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    @Bean
    JdbcIndexedSessionRepository sessionRepository(DataSource dataSource) {
        var transactionManager = new DataSourceTransactionManager(dataSource);
        var repository = new JdbcIndexedSessionRepository(new JdbcTemplate(dataSource),
                new TransactionTemplate(transactionManager));
        repository.setTableName("identity.spring_session");
        repository.setDefaultMaxInactiveInterval(IDLE_TIMEOUT);
        return repository;
    }

    @Bean
    SessionLimiter sessionLimiter(JdbcIndexedSessionRepository sessionRepository) {
        return new JdbcSessionLimiter(sessionRepository);
    }

    @Bean
    FilterRegistrationBean<SessionRepositoryFilter<?>> springSessionFilter(
            DataSource dataSource, JdbcIndexedSessionRepository sessionRepository) {
        var resolver = new CookieHttpSessionIdResolver();
        resolver.setCookieSerializer(sessionCookieSerializer());
        var transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var filter = new SessionRepositoryFilter<>(
                new CappedSessionRepository(dataSource, sessionRepository, transactions));
        filter.setHttpSessionIdResolver(resolver);
        var registration = new FilterRegistrationBean<SessionRepositoryFilter<?>>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return registration;
    }

    @Bean
    FilterRegistrationBean<Filter> sessionStoreFailureFilter(AuthProblems problems) {
        var registration = new FilterRegistrationBean<Filter>(new SessionStoreFailureFilter(problems));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    private static DefaultCookieSerializer sessionCookieSerializer() {
        var serializer = new DefaultCookieSerializer();
        serializer.setCookieName(AuthProblems.SESSION_COOKIE);
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        // Local qualification runs over plain HTTP; production deployment must require TLS.
        serializer.setUseSecureCookie(false);
        return serializer;
    }
}
