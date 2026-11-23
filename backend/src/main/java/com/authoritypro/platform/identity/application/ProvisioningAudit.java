package com.authoritypro.platform.identity.application;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/** Control-plane audit port for the bootstrap action; implemented without an inter-module dependency. */
public interface ProvisioningAudit {
    void operatorProvisioned(Connection connection, UUID operatorUserId, UUID operationId) throws SQLException;
}
