export interface Company {
  companyId: string;
  name: string;
}

export interface Membership {
  tenantId: string;
  tenantName: string;
  roles: string[];
  companies: Company[];
}

export interface OwnProfile {
  userId: string;
  displayName: string;
  login: string;
  memberships: Membership[];
}

export interface ScopedAccess {
  tenantId: string;
  tenantName: string;
  companyId: string | null;
  companyName: string | null;
  authorizationRevision: number;
  roles: string[];
  permissions: string[];
}

export interface ModuleDependency {
  id: string;
  version: string;
}

export interface ModuleCapability {
  id: string;
  version: string;
  contractType: string;
}

export interface ModuleCapabilities {
  provides: ModuleCapability[];
  requires: ModuleCapability[];
}

export interface ModulePermission {
  id: string;
  scope: string;
}

export interface OwnedResource {
  name: string;
  scope: string;
}

export interface ModuleView {
  id: string;
  version: string;
  digest: string;
  lifecycle: string;
  availabilityReason: string;
  dependencies: ModuleDependency[];
  capabilities: ModuleCapabilities;
  permissions: ModulePermission[];
  dataOwnership: OwnedResource[];
}

export interface InventoryView {
  buildRevision: string;
  modules: ModuleView[];
}

export interface AuditRecord {
  recordId: string;
  occurredAt: string;
  actionCode: string;
  actionVersion: number;
  actorId: string;
  actorKind: string;
  targetType: string | null;
  targetId: string | null;
  operationId: string;
  correlationId: string;
  schemaVersion: number;
  metadata: Record<string, unknown>;
  outcome: string;
  tenantId: string;
  companyId: string | null;
  siteId: string | null;
}

export interface AuditPage {
  items: AuditRecord[];
  nextOccurredAt: string | null;
  nextRecordId: string | null;
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  traceId?: string;
}
