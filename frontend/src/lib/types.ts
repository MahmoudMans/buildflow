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
