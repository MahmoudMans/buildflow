export interface ProjectActivity {
  id: string;
  projectId: string;
  actorUserId: string | null;
  action: string;
  message: string;
  entityType: string | null;
  entityId: string | null;
  createdAt: string;
}
