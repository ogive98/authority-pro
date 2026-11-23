package com.authoritypro.platform.identity.infrastructure.persistence;

import com.authoritypro.platform.AuthorityProApplication.OperatorProvisioner;
import com.authoritypro.platform.identity.application.OperatorProvisioning;
import com.authoritypro.platform.identity.domain.LoginName;
import com.authoritypro.platform.identity.infrastructure.security.SpringPasswordHasher;
import java.sql.Connection;
import java.util.UUID;

/** ServiceLoader entrypoint for `provision-local`; the caller owns commit and rollback. */
public final class LocalOperatorProvisioner implements OperatorProvisioner {
    @Override
    public void provision(Connection connection, String login, char[] password) {
        LoginName name = LoginName.of(login);
        new OperatorProvisioning(new JdbcProvisioningStore(), new SpringPasswordHasher(), new JdbcProvisioningAudit())
                .provision(connection, name, name.value(), password, UUID.randomUUID());
    }
}
