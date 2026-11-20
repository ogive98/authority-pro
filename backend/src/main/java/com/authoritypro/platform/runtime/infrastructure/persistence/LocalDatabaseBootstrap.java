package com.authoritypro.platform.runtime.infrastructure.persistence;

import com.authoritypro.platform.AuthorityProApplication.DatabaseBootstrap;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;

public final class LocalDatabaseBootstrap implements DatabaseBootstrap {
    @Override
    public HikariDataSource runtime(InputStream input) {
        return LocalDatabase.createRuntime(LocalDatabase.readPassword(input));
    }

    @Override
    public int migrate(InputStream input) {
        return LocalDatabase.migrate(LocalDatabase.readPassword(input));
    }
}
