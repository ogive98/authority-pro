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

    @Override
    public String[] provisionerSecrets(InputStream input) {
        return LocalDatabase.readSecrets(input, 3);
    }

    @Override
    public HikariDataSource provisioner(String password) {
        return LocalDatabase.provisionerDataSource(LocalDatabase.selectedDatabase(), password);
    }
}
