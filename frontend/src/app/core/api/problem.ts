import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from '../models';

export type FailureKind =
  | 'authentication'
  | 'throttled'
  | 'unavailable'
  | 'denied'
  | 'invalid'
  | 'network'
  | 'unknown';

export interface ApiFailure {
  kind: FailureKind;
  status: number;
  code: string | null;
  traceId: string | null;
  message: string;
}

const MESSAGES: Record<FailureKind, string> = {
  authentication: 'La session n’est pas valide. Reconnectez-vous.',
  throttled: 'Trop de tentatives. Réessayez dans un instant.',
  unavailable: 'Le service est temporairement indisponible.',
  denied: 'L’accès demandé est refusé.',
  invalid: 'La sélection de portée est invalide.',
  network: 'Le service est injoignable. Vérifiez votre connexion.',
  unknown: 'Une erreur inattendue est survenue.',
};

export function toFailure(error: unknown): ApiFailure {
  if (!(error instanceof HttpErrorResponse)) {
    return { kind: 'unknown', status: 0, code: null, traceId: null, message: MESSAGES.unknown };
  }
  const body = (error.error ?? {}) as ProblemDetail;
  const code = typeof body.code === 'string' ? body.code : null;
  const traceId = typeof body.traceId === 'string' ? body.traceId : null;
  const status = error.status;
  if (status === 0) return failure('network', 0, code, traceId);
  if (status === 401) return failure('authentication', 401, code, traceId);
  if (status === 403) return failure('denied', 403, code, traceId);
  if (status === 400) return failure('invalid', 400, code, traceId);
  if (status === 429) return failure('throttled', 429, code, traceId);
  if (status === 503) return failure('unavailable', 503, code, traceId);
  return failure('unknown', status, code, traceId);
}

function failure(
  kind: FailureKind,
  status: number,
  code: string | null,
  traceId: string | null,
): ApiFailure {
  const message =
    kind === 'denied' && code === 'CSRF_REJECTED'
      ? 'La requête n’a pas pu être vérifiée. Rechargez la page.'
      : MESSAGES[kind];
  return { kind, status, code, traceId, message };
}
