// Shapes returned by GET /audit/search and GET /audit/facets.

export interface AuditEntry {
  id: string;
  projectId: string | null;
  projectName: string | null;
  projectNumber: string | null;
  actorUserId: string | null;
  actorName: string | null;
  actorEmail: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  oldValues: string | null;
  newValues: string | null;
  ipAddress: string | null;
  createdAt: string;
}

// Newest first. page starts at 0.
export interface AuditPage {
  items: AuditEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditActorOption {
  userId: string;
  name: string | null;
  email: string;
}

export interface AuditProjectOption {
  projectId: string;
  name: string;
  projectNumber: string;
}

// The values that exist in this organization's log.
export interface AuditFacets {
  actions: string[];
  entityTypes: string[];
  actors: AuditActorOption[];
  projects: AuditProjectOption[];
}

// What the person has chosen. An empty string means "no filter".
// from and to are days written like 2026-03-31.
export interface AuditFilters {
  q: string;
  projectId: string;
  actorUserId: string;
  entityType: string;
  action: string;
  from: string;
  to: string;
}
