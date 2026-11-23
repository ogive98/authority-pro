package com.authoritypro.platform.identity.application;

import com.authoritypro.platform.identity.domain.LoginName;
import com.authoritypro.platform.identity.domain.ProvisioningRefusedException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * One-shot initial-operator creation. The caller owns the transaction: guard, user, credential and
 * control-plane audit either commit together or roll back together. No tenant membership is created.
 */
public final class OperatorProvisioning {
    private final ProvisioningStore store;
    private final PasswordHasher hasher;
    private final ProvisioningAudit audit;

    public OperatorProvisioning(ProvisioningStore store, PasswordHasher hasher, ProvisioningAudit audit) {
        if (store == null || hasher == null || audit == null) throw new ProvisioningRefusedException();
        this.store = store;
        this.hasher = hasher;
        this.audit = audit;
    }

    public UUID provision(Connection connection, LoginName login, String displayName, char[] password,
            UUID operationId) {
        if (connection == null || login == null || displayName == null || password == null
                || password.length == 0 || operationId == null) {
            throw new ProvisioningRefusedException();
        }
        try {
            if (store.guardPresent(connection)) throw new ProvisioningRefusedException();
            UUID userId = UUID.randomUUID();
            String hash = hasher.encode(password);
            store.insertUser(connection, userId, login.value(), displayName);
            store.insertCredential(connection, userId, hash);
            store.insertGuard(connection, userId);
            audit.operatorProvisioned(connection, userId, operationId);
            return userId;
        } catch (SQLException failure) {
            throw new ProvisioningRefusedException();
        }
    }
}
