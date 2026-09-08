export type OrganizationRole =
  | "OWNER"
  | "ADMIN"
  | "PROJECT_MANAGER"
  | "SITE_MANAGER"
  | "WORKER"
  | "ACCOUNTANT"
  | "CLIENT";

export type MembershipStatus =
  | "ACTIVE"
  | "INACTIVE";

export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  createdAt: string;
}

export interface OrganizationSummary {
  id: string;
  name: string;
  currency: string;
  timezone: string;
  role: OrganizationRole;
}

export interface AuthResponse {
  user: User;
  organizations: OrganizationSummary[];
}

export interface Member {
  membershipId: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: OrganizationRole;
  status: MembershipStatus;
  joinedAt: string;
}

export interface Invitation {
  id: string;
  email: string;
  role: OrganizationRole;
  status:
    | "PENDING"
    | "ACCEPTED"
    | "EXPIRED"
    | "REVOKED";
  expiresAt: string;
  createdAt: string;
}

export interface ApiErrorBody {
  code?: string;
  message?: string;
}

export type ClientType =
  | "INDIVIDUAL"
  | "COMPANY";

export type ClientStatus =
  | "ACTIVE"
  | "INACTIVE"
  | "ARCHIVED";

export interface ClientSummary {
  id: string;
  type: ClientType;
  displayName: string;
  email: string | null;
  phone: string | null;
  status: ClientStatus;
  createdAt: string;
}

export interface ClientContact {
  id: string;
  firstName: string;
  lastName: string;
  jobTitle: string | null;
  email: string | null;
  phone: string | null;
  primary: boolean;
  createdAt: string;
}

export interface ClientView {
  id: string;
  type: ClientType;
  companyName: string | null;
  firstName: string | null;
  lastName: string | null;
  displayName: string;
  email: string | null;
  phone: string | null;
  taxNumber: string | null;
  registrationNumber: string | null;
  billingAddress: string | null;
  siteAddress: string | null;
  notes: string | null;
  status: ClientStatus;
  createdAt: string;
  contacts: ClientContact[];
}

export type ProjectType =
  | "NEW_CONSTRUCTION"
  | "RENOVATION"
  | "INTERIOR"
  | "ELECTRICAL"
  | "PLUMBING"
  | "ROOFING"
  | "LANDSCAPING"
  | "MAINTENANCE"
  | "OTHER";

export type ProjectStatus =
  | "DRAFT"
  | "PLANNING"
  | "ACTIVE"
  | "ON_HOLD"
  | "COMPLETED"
  | "CANCELLED"
  | "ARCHIVED";

export type ProjectPriority =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export type ProjectRole =
  | "PROJECT_MANAGER"
  | "SITE_MANAGER"
  | "ENGINEER"
  | "FOREMAN"
  | "WORKER"
  | "ACCOUNTANT"
  | "VIEWER";

export interface ProjectSummary {
  id: string;
  projectNumber: string;
  name: string;
  clientName: string;
  projectType: ProjectType;
  status: ProjectStatus;
  priority: ProjectPriority;
  startDate: string | null;
  plannedEndDate: string | null;
  progressPercentage: number;
  currency: string;
  createdAt: string;
}

export interface ProjectView {
  id: string;
  clientId: string;
  clientName: string;
  projectNumber: string;
  name: string;
  description: string | null;
  projectType: ProjectType;
  status: ProjectStatus;
  priority: ProjectPriority;
  siteAddress: string | null;
  city: string | null;
  postalCode: string | null;
  country: string | null;
  startDate: string | null;
  plannedEndDate: string | null;
  actualEndDate: string | null;
  currency: string;
  contractValue: number;
  originalBudget: number;
  projectManagerMembershipId: string | null;
  progressPercentage: number;
  createdAt: string;
  archivedAt: string | null;
}

export interface ProjectMember {
  id: string;
  membershipId: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  projectRole: ProjectRole;
  joinedAt: string;
}

export type TaskStatus =
  | "TODO"
  | "IN_PROGRESS"
  | "BLOCKED"
  | "REVIEW"
  | "DONE"
  | "CANCELLED";

export type TaskPriority =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

export interface TaskAssignment {
  membershipId: string;
  userId: string;
  firstName: string;
  lastName: string;
  email: string;
  assignedAt: string;
}

export interface TaskSummary {
  id: string;
  milestoneId: string | null;
  parentTaskId: string | null;
  title: string;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: string | null;
  dueDate: string | null;
  estimatedHours: number | null;
  actualHours: number;
  progressPercentage: number;
  weight: number;
  assignments: TaskAssignment[];
}

export interface TaskView {
  id: string;
  projectId: string;
  milestoneId: string | null;
  parentTaskId: string | null;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  startDate: string | null;
  dueDate: string | null;
  estimatedHours: number | null;
  actualHours: number;
  progressPercentage: number;
  weight: number;
  createdBy: string;
  createdAt: string;
  completedAt: string | null;
  assignments: TaskAssignment[];
}

export interface TaskComment {
  id: string;
  authorMembershipId: string;
  authorUserId: string;
  authorName: string;
  content: string;
  createdAt: string;
  editedAt: string | null;
  edited: boolean;
}

export interface TaskWorkLog {
  id: string;
  membershipId: string;
  userId: string;
  memberName: string;
  workDate: string;
  minutesWorked: number;
  description: string | null;
  createdAt: string;
}

export type MilestoneStatus =
  | "UPCOMING"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "MISSED"
  | "CANCELLED";

export interface Milestone {
  id: string;
  projectId: string;
  name: string;
  description: string | null;
  plannedDate: string | null;
  completedDate: string | null;
  status: MilestoneStatus;
  progressPercentage: number;
  clientVisible: boolean;
  createdAt: string;
}
