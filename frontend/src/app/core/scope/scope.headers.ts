export function scopeHeaders(scope: { tenantId: string; companyId?: string | null }): Record<string, string> {
  const headers: Record<string, string> = { 'X-Authority-Tenant': scope.tenantId };
  if (scope.companyId) headers['X-Authority-Company'] = scope.companyId;
  return headers;
}
