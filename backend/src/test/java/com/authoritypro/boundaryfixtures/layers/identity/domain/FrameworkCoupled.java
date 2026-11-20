package com.authoritypro.boundaryfixtures.layers.identity.domain;

import org.springframework.context.ApplicationContext;

public record FrameworkCoupled(ApplicationContext context, java.sql.Connection connection,
        javax.sql.DataSource source, com.zaxxer.hikari.HikariDataSource pool,
        org.flywaydb.core.Flyway migrations, org.postgresql.ds.PGSimpleDataSource driver) {
}
