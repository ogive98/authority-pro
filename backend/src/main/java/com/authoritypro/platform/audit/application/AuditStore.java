package com.authoritypro.platform.audit.application;

import com.authoritypro.platform.audit.domain.AuditEvent;
import com.authoritypro.platform.audit.domain.AuditFamily;
import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import com.authoritypro.platform.audit.domain.FailedAttempt;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

/**
 * Append joins the caller's already-authorized transaction on the same connection.
 * Read callers must already hold the approved audit or attempts permission.
 */
public interface AuditStore {
    AuditRecordView appendCritical(Connection connection, AuditEvent event);

    List<AuditRecordView> read(Connection connection, AuditFamily family, AuditQuery query);

    UUID recordFailedAttempt(Connection connection, FailedAttempt attempt);
}
