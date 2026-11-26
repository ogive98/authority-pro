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

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  traceId?: string;
}
