export type SitePhotoCategory =
  | "BEFORE"
  | "PROGRESS"
  | "AFTER"
  | "DELIVERY"
  | "DAMAGE"
  | "SAFETY_ISSUE"
  | "OTHER";

export type IncidentType =
  | "SAFETY"
  | "MATERIAL_DAMAGE"
  | "SITE_ISSUE"
  | "QUALITY"
  | "DELAY"
  | "EQUIPMENT";

export type IncidentSeverity =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export type IncidentStatus =
  | "OPEN"
  | "INVESTIGATING"
  | "ACTION_REQUIRED"
  | "RESOLVED"
  | "CLOSED";

export interface DailySiteReport {
  id: string;
  projectId: string;
  reportDate: string;
  weatherText: string | null;
  workersOnSite: number;
  summary: string;
  workCompleted: string | null;
  delays: string | null;
  issues: string | null;
  nextSteps: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface SitePhoto {
  id: string;
  reportId: string;
  storageKey: string;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  caption: string | null;
  category: SitePhotoCategory;
  clientVisible: boolean;
  createdBy: string;
  createdAt: string;
}

export interface SiteIncident {
  id: string;
  projectId: string;
  incidentNumber: string;
  type: IncidentType;
  severity: IncidentSeverity;
  title: string;
  description: string;
  occurredAt: string;
  reportedAt: string;
  status: IncidentStatus;
  reportedBy: string;
  assignedTo: string | null;
  resolution: string | null;
  resolvedAt: string | null;
  clientVisible: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProjectProgressUpdate {
  id: string;
  projectId: string;
  progressPercentageSnapshot: number;
  summary: string;
  blockers: string | null;
  nextSteps: string | null;
  createdBy: string;
  createdAt: string;
}
