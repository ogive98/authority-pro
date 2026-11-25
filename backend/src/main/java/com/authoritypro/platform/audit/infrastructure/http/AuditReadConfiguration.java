package com.authoritypro.platform.audit.infrastructure.http;

import com.authoritypro.platform.audit.application.AuditRecordReader;
import com.authoritypro.platform.audit.infrastructure.persistence.JdbcAuditRecordReader;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The tenant audit read port exists only when the runtime datasource is installed. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(DataSource.class)
public class AuditReadConfiguration {

    @Bean
    AuditRecordReader auditRecordReader(DataSource dataSource) {
        return new JdbcAuditRecordReader(dataSource);
    }
}
