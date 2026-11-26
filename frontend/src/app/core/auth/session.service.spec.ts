import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { OwnProfile } from '../models';
import { SessionService } from './session.service';

const profile: OwnProfile = {
  userId: '11111111-1111-4111-8111-111111111111',
  displayName: 'Opérateur',
  login: 'operator',
  memberships: [],
};

const tick = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('SessionService', () => {
  let service: SessionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SessionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('restores an authenticated profile', async () => {
    const restored = service.restore();
    http.expectOne('/api/v1/auth/me').flush(profile);
    await expect(restored).resolves.toBe(true);
    expect(service.state()).toBe('authenticated');
    expect(service.profile()?.displayName).toBe('Opérateur');
  });

  it('treats 401 as anonymous', async () => {
    const restored = service.restore();
    http
      .expectOne('/api/v1/auth/me')
      .flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    await expect(restored).resolves.toBe(false);
    expect(service.state()).toBe('anonymous');
  });

  it('does not treat 403 as a logout', async () => {
    const restored = service.restore();
    http
      .expectOne('/api/v1/auth/me')
      .flush({ code: 'AUTHORIZATION_DENIED' }, { status: 403, statusText: 'Forbidden' });
    await expect(restored).resolves.toBe(false);
    expect(service.state()).toBe('denied');
  });

  it('reports 503 as unavailable', async () => {
    const restored = service.restore();
    http
      .expectOne('/api/v1/auth/me')
      .flush({ code: 'SESSION_STORE_UNAVAILABLE' }, { status: 503, statusText: 'Unavailable' });
    await expect(restored).resolves.toBe(false);
    expect(service.state()).toBe('unavailable');
  });

  it('requests CSRF, then login, then the profile', async () => {
    const result = service.login('operator', 'secret');
    http.expectOne('/api/v1/auth/csrf').flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    const login = http.expectOne('/api/v1/auth/login');
    expect(login.request.body).toEqual({ login: 'operator', password: 'secret' });
    login.flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    http.expectOne('/api/v1/auth/me').flush(profile);
    await expect(result).resolves.toEqual({ ok: true });
    expect(service.state()).toBe('authenticated');
  });

  it('maps a refused login to an authentication failure', async () => {
    const result = service.login('operator', 'wrong');
    http.expectOne('/api/v1/auth/csrf').flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    http
      .expectOne('/api/v1/auth/login')
      .flush({ code: 'AUTHENTICATION_REQUIRED' }, { status: 401, statusText: 'Unauthorized' });
    const value = await result;
    expect(value.ok).toBe(false);
    if (!value.ok) expect(value.failure.kind).toBe('authentication');
    expect(service.state()).toBe('anonymous');
  });

  it('maps throttling to a throttled failure', async () => {
    const result = service.login('operator', 'wrong');
    http.expectOne('/api/v1/auth/csrf').flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    http
      .expectOne('/api/v1/auth/login')
      .flush({ code: 'AUTHENTICATION_THROTTLED' }, { status: 429, statusText: 'Too Many Requests' });
    const value = await result;
    expect(value.ok).toBe(false);
    if (!value.ok) expect(value.failure.kind).toBe('throttled');
  });

  it('clears the local session on logout even when the service fails', async () => {
    service.clear();
    const logout = service.logout();
    http.expectOne('/api/v1/auth/csrf').flush(null, { status: 204, statusText: 'No Content' });
    await tick();
    http
      .expectOne('/api/v1/auth/logout')
      .flush({ code: 'SESSION_STORE_UNAVAILABLE' }, { status: 503, statusText: 'Unavailable' });
    await logout;
    expect(service.state()).toBe('anonymous');
  });
});
