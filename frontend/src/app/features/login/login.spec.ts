import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginResult, SessionService } from '../../core/auth/session.service';
import { LoginPage } from './login';

function sessionStub(login: (login: string, password: string) => Promise<LoginResult>) {
  return {
    state: signal<'anonymous' | 'unavailable' | 'denied'>('anonymous'),
    profile: signal(null),
    login: vi.fn(login),
    logout: vi.fn(async () => undefined),
    restore: vi.fn(async () => false),
  };
}

async function render(login: (login: string, password: string) => Promise<LoginResult>) {
  const session = sessionStub(login);
  await TestBed.configureTestingModule({
    imports: [LoginPage],
    providers: [provideRouter([]), { provide: SessionService, useValue: session }],
  }).compileComponents();
  const fixture = TestBed.createComponent(LoginPage);
  fixture.detectChanges();
  return { fixture, session, component: fixture.componentInstance };
}

describe('LoginPage', () => {
  beforeEach(() => TestBed.resetTestingModule());

  it('does not submit an invalid form', async () => {
    const { component, session } = await render(async () => ({ ok: true }));
    component.submit();
    expect(session.login).not.toHaveBeenCalled();
    expect(component.form.controls.login.touched).toBe(true);
    expect(component.form.controls.password.touched).toBe(true);
  });

  it('shows the generic failure for refused credentials', async () => {
    const { fixture, component } = await render(async () => ({
      ok: false,
      failure: {
        kind: 'authentication',
        status: 401,
        code: 'AUTHENTICATION_REQUIRED',
        traceId: 'abc123',
        message: 'La session n’est pas valide. Reconnectez-vous.',
      },
    }));
    component.form.setValue({ login: 'operator', password: 'wrong' });
    component.submit();
    await fixture.whenStable();
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert.textContent).toContain('La session n’est pas valide');
    expect(alert.textContent).toContain('abc123');
    expect(component.submitting()).toBe(false);
  });

  it('shows throttling as a distinct message', async () => {
    const { fixture, component } = await render(async () => ({
      ok: false,
      failure: {
        kind: 'throttled',
        status: 429,
        code: 'AUTHENTICATION_THROTTLED',
        traceId: null,
        message: 'Trop de tentatives. Réessayez dans un instant.',
      },
    }));
    component.form.setValue({ login: 'operator', password: 'wrong' });
    component.submit();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Trop de tentatives');
  });

  it('moves focus to the failure alert once it is rendered', async () => {
    const { fixture, component } = await render(async () => ({
      ok: false,
      failure: {
        kind: 'unavailable',
        status: 503,
        code: 'ACCESS_UNAVAILABLE',
        traceId: null,
        message: 'Le service est indisponible.',
      },
    }));
    component.form.setValue({ login: 'operator', password: 'wrong' });
    component.submit();
    await fixture.whenStable();
    fixture.detectChanges();
    const alert = fixture.nativeElement.querySelector('[role="alert"]') as HTMLElement;
    expect(alert).not.toBeNull();
    expect(document.activeElement).toBe(alert);
  });

  it('prevents a double submission', async () => {
    let settle: (value: LoginResult) => void = () => undefined;
    const pending = new Promise<LoginResult>((resolve) => {
      settle = resolve;
    });
    const { component, session } = await render(() => pending);
    component.form.setValue({ login: 'operator', password: 'secret' });
    component.submit();
    component.submit();
    expect(session.login).toHaveBeenCalledTimes(1);
    settle({ ok: false, failure: { kind: 'unavailable', status: 503, code: null, traceId: null, message: 'x' } });
    await Promise.resolve();
  });

  it('toggles password visibility without submitting', async () => {
    const { fixture, component, session } = await render(async () => ({ ok: true }));
    const input = fixture.nativeElement.querySelector('#password') as HTMLInputElement;
    expect(input.type).toBe('password');
    component.togglePassword();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(session.login).not.toHaveBeenCalled();
  });
});
