export type Role = 'SOC_ANALYST' | 'INCIDENT_MANAGER' | 'AUDITOR' | 'SYSTEM_INTEGRATION' | string;

export interface Session {
  accessToken: string;
  username: string;
  roles: Role[];
  expiresAt: number;
}

export interface Incident {
  incidentId: string;
  sourceAlertId?: string | null;
  title: string;
  description: string;
  incidentType: string;
  natureOfViolation: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
  status: 'NEW' | 'TRIAGED' | 'INVESTIGATING' | 'CONTAINMENT' | 'RECOVERY' | 'RESOLVED' | 'CLOSED' | string;
  classificationStatus: string;
  reportedBy: string;
  assignedTo?: string | null;
  businessUnit?: string | null;
  policyReference?: string | null;
  occurredAt?: string | null;
  detectedAt?: string | null;
  reportedAt?: string | null;
  fiscalYear?: string | null;
  reportingQuarter?: string | null;
  businessImpact?: string | null;
  rootCause?: string | null;
  correctiveAction?: string | null;
  resolvedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface QuarterlyReport {
  fiscalYear: string;
  quarter: string;
  totalIncidents: number;
  countsBySeverity: Record<string, number>;
  countsByStatus: Record<string, number>;
  countsByViolation: Record<string, number>;
}

export interface Alert {
  alertId: string;
  externalAlertId: string;
  source: string;
  title: string;
  incidentType: string;
  natureOfViolation: string;
  severity: string;
  status: string;
  createdIncidentId?: string | null;
  lastError?: string | null;
  reportedAt?: string | null;
  processedAt?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface AuditEvent {
  id: number;
  incidentId: string;
  action: string;
  oldValue?: string | null;
  newValue?: string | null;
  performedBy?: string | null;
  rationale?: string | null;
  occurredAt?: string | null;
}

export interface Relationship {
  id?: number;
  sourceIncidentId?: string;
  relatedIncidentId?: string;
  relationshipType: string;
  createdBy?: string;
  createdAt?: string;
}
