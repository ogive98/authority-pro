import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiFailure, toFailure } from '../api/problem';
import { OwnProfile } from '../models';

export type SessionState = 'unknown' | 'anonymous' | 'authenticated' | 'denied' | 'unavailable';

export type LoginResult = { ok: true } | { ok: false; failure: ApiFailure };

type ProfileRead = { ok: true; profile: OwnProfile } | { ok: false; failure: ApiFailure };

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly stateSignal = signal<SessionState>('unknown');
  private readonly profileSignal = signal<OwnProfile | null>(null);
  private restorePromise: Promise<boolean> | null = null;
  private generation = 0;

  readonly state = this.stateSignal.asReadonly();
  readonly profile = this.profileSignal.asReadonly();

  constructor(private readonly http: HttpClient) {}

  restore(): Promise<boolean> {
    if (this.stateSignal() === 'authenticated' && this.profileSignal()) {
      return Promise.resolve(true);
    }
    if (this.restorePromise) return this.restorePromise;
    const generation = this.generation;
    this.restorePromise = (async () => {
      const read = await this.readProfile();
      if (generation !== this.generation) return false;
      if (read.ok) {
        this.profileSignal.set(read.profile);
        this.stateSignal.set('authenticated');
        return true;
      }
      this.profileSignal.set(null);
      this.stateSignal.set(stateForFailure(read.failure));
      return false;
    })().finally(() => {
      this.restorePromise = null;
    });
    return this.restorePromise;
  }

  async login(login: string, password: string): Promise<LoginResult> {
    const generation = this.generation;
    try {
      await this.ensureCsrf();
      await firstValueFrom(this.http.post<void>('/api/v1/auth/login', { login, password }));
    } catch (error) {
      const failure = toFailure(error);
      if (failure.status === 401 || failure.status === 429) {
        this.profileSignal.set(null);
        this.stateSignal.set('anonymous');
      } else if (failure.status === 503 || failure.kind === 'network') {
        this.stateSignal.set('unavailable');
      }
      return { ok: false, failure };
    }
    const read = await this.readProfile();
    if (generation !== this.generation) {
      return {
        ok: false,
        failure: {
          kind: 'unknown',
          status: 0,
          code: null,
          traceId: null,
          message: 'La connexion a été interrompue.',
        },
      };
    }
    if (!read.ok) {
      this.profileSignal.set(null);
      this.stateSignal.set(stateForFailure(read.failure));
      return read;
    }
    this.profileSignal.set(read.profile);
    this.stateSignal.set('authenticated');
    return { ok: true };
  }

  async logout(): Promise<void> {
    try {
      await this.ensureCsrf();
      await firstValueFrom(this.http.post<void>('/api/v1/auth/logout', null));
    } catch {
      // The local session is cleared even when the store cannot confirm the invalidation.
    } finally {
      this.clear();
    }
  }

  clear(): void {
    this.generation += 1;
    this.restorePromise = null;
    this.profileSignal.set(null);
    this.stateSignal.set('anonymous');
  }

  private async readProfile(): Promise<ProfileRead> {
    try {
      const profile = await firstValueFrom(this.http.get<OwnProfile>('/api/v1/auth/me'));
      return { ok: true, profile };
    } catch (error) {
      return { ok: false, failure: toFailure(error) };
    }
  }

  private async ensureCsrf(): Promise<void> {
    await firstValueFrom(this.http.get('/api/v1/auth/csrf', { observe: 'response' }));
  }
}

function stateForFailure(failure: ApiFailure): SessionState {
  if (failure.status === 401) return 'anonymous';
  if (failure.status === 403) return 'denied';
  return 'unavailable';
}
