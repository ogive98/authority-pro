package com.authoritypro.platform.audit.application;

import com.authoritypro.platform.audit.domain.AuditQuery;
import com.authoritypro.platform.audit.domain.AuditRecordView;
import java.util.List;
import java.util.UUID;

/**
 * Reads tenant audit records inside a fresh transaction scoped to the already-authorized
 * selection. Authorization belongs to the HTTP boundary, never to this port.
 */
public interface AuditRecordReader {

    List<AuditRecordView> read(UUID userId, UUID tenantId, UUID companyId, AuditQuery query);
}
