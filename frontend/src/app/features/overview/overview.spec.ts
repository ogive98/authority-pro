import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { ScopedAccess } from '../../core/models';
import { ScopeService } from '../../core/scope/scope.service';
import { OverviewPage } from './overview';

const access: ScopedAccess = {
  tenantId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  tenantName: 'Tenant A',
  companyId: 'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
  companyName: 'Société A1',
  authorizationRevision: 3,
  roles: ['ROLE_OWNER'],
  permissions: ['audit.read.tenant'],
};

function render(scope: ScopedAccess | null) {
  TestBed.configureTestingModule({
    imports: [OverviewPage],
    providers: [
      provideRouter([]),
      {
        provide: ScopeService,
        useValue: {
          scope: signal(scope).asReadonly(),
          status: signal(scope ? 'ready' : 'idle').asReadonly(),
          failure: signal(null).asReadonly(),
          select: () => undefined,
          clear: () => undefined,
        },
      },
    ],
  });
  const fixture = TestBed.createComponent(OverviewPage);
  fixture.detectChanges();
  return fixture;
}

describe('OverviewPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('shows only data returned by the bootstrap', () => {
    const fixture = render(access);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Tenant A');
    expect(text).toContain('Société A1');
    expect(text).toContain('ROLE_OWNER');
    expect(text).toContain('audit.read.tenant');
    expect(text).toContain('3');
  });

  it('states that inventory and audit are not wired yet', () => {
    const fixture = render(access);
    expect(fixture.nativeElement.textContent).toContain('lots P4 et P5');
  });

  it('offers a safe path when no scope is active', () => {
    const fixture = render(null);
    expect(fixture.nativeElement.textContent).toContain('Aucune portée active');
  });
});
