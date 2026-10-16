import type { Alert, AuditEvent, Incident, PageResult, QuarterlyReport, Relationship, Session } from './types';

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, session?: Session | null, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (session?.accessToken) headers.set('Authorization', `Bearer ${session.accessToken}`);

  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (response.status === 204) return undefined as T;
  const raw = await response.text();
  let body: unknown;
  try { body = raw ? JSON.parse(raw) : undefined; } catch { body = raw; }
  if (!response.ok) {
    const maybe = body as { message?: string; error?: string; detail?: string } | undefined;
    const message = maybe?.message || maybe?.detail || maybe?.error || (typeof body === 'string' ? body : `Request failed (${response.status})`);
    throw new ApiError(response.status, message);
  }
  return body as T;
}

export async function login(username: string, password: string): Promise<Session> {
  const result = await request<{ accessToken: string; expiresIn: number; username: string; roles: string[] }>('/api/v1/auth/login', null, {
    method: 'POST', body: JSON.stringify({ username, password }),
  });
  return {
    accessToken: result.accessToken,
    username: result.username,
    roles: result.roles ?? [],
    expiresAt: Date.now() + (result.expiresIn ?? 1800) * 1000,
  };
}

export function getIncidents(session: Session, query: { severity?: string; status?: string; natureOfViolation?: string; incidentType?: string; fiscalYear?: string; quarter?: string; page?: number; size?: number } = {}) {
  const params = new URLSearchParams();
  Object.entries({ page: 0, size: 100, ...query }).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') params.set(key, String(value));
  });
  return request<PageResult<Incident>>(`/api/v1/incidents?${params.toString()}`, session);
}

export function createIncident(session: Session, payload: Record<string, unknown>) {
  return request<Incident>('/api/v1/incidents', session, { method: 'POST', body: JSON.stringify(payload) });
}

export function updateSeverity(session: Session, id: string, severity: string, rationale: string) {
  return request<Incident>(`/api/v1/incidents/${encodeURIComponent(id)}/severity`, session, {
    method: 'PATCH', body: JSON.stringify({ severity, performedBy: session.username, rationale }),
  });
}

export function updateStatus(session: Session, id: string, status: string, rationale: string) {
  return request<Incident>(`/api/v1/incidents/${encodeURIComponent(id)}/status`, session, {
    method: 'PATCH', body: JSON.stringify({ status, performedBy: session.username, rationale }),
  });
}

export function getAuditEvents(session: Session, id: string) {
  return request<AuditEvent[]>(`/api/v1/incidents/${encodeURIComponent(id)}/audit-events`, session);
}

export function getRelationships(session: Session, id: string) {
  return request<Relationship[]>(`/api/v1/incidents/${encodeURIComponent(id)}/relationships`, session);
}

export function getQuarterlyReport(session: Session, fiscalYear: string, quarter: string) {
  return request<QuarterlyReport>(`/api/v1/reports/quarterly?fiscalYear=${encodeURIComponent(fiscalYear)}&quarter=${encodeURIComponent(quarter)}`, session);
}

export function getAlerts(session: Session) {
  return request<Alert[]>('/api/v1/alerts', session);
}

export function submitAlert(session: Session, payload: Record<string, unknown>) {
  return request<Alert>('/api/v1/alerts', session, { method: 'POST', body: JSON.stringify(payload) });
}

export async function getReferences(session: Session) {
  const [severities, statuses, classificationStatuses, violationCategories] = await Promise.all([
    request<string[]>('/api/v1/reference/severities', session),
    request<string[]>('/api/v1/reference/statuses', session),
    request<string[]>('/api/v1/reference/classification-statuses', session),
    request<{ code: string; label: string; description: string }[]>('/api/v1/reference/violation-categories', session),
  ]);
  return { severities, statuses, classificationStatuses, violationCategories };
}
