package com.authoritypro.platform.identity.application;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/** Provisioning persistence port; every call joins the caller's bootstrap transaction. */
public interface ProvisioningStore {
    boolean guardPresent(Connection connection) throws SQLException;

    void insertGuard(Connection connection, UUID operatorUserId) throws SQLException;

    void insertUser(Connection connection, UUID userId, String loginNormalized, String displayName) throws SQLException;

    void insertCredential(Connection connection, UUID userId, String passwordHash) throws SQLException;
}
