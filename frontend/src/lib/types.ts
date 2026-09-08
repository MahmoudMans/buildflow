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

export interface BudgetItem {
  id: string;
  categoryId: string;
  description: string;
  quantity: number;
  unitCost: number;
  plannedAmount: number;
  notes: string | null;
  createdAt: string;
}

export interface BudgetCategory {
  id: string;
  name: string;
  description: string | null;
  sortOrder: number;
  plannedAmount: number;
  items: BudgetItem[];
  createdAt: string;
}

export interface BudgetView {
  projectId: string;
  baselineLocked: boolean;
  plannedBudget: number;
  originalBudget: number;
  categories: BudgetCategory[];
}

export interface FinancialSummary {
  projectId: string;
  budgetBaselineLocked: boolean;
  plannedBudget: number;
  originalBudget: number;
  revisedBudget: number;
  actualCost: number;
  committedCost: number;
  forecastCost: number;
  budgetRemaining: number;
  forecastBudgetVariance: number;
}

export type ExpenseStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "APPROVED"
  | "REJECTED"
  | "VOID";

export interface Expense {
  id: string;
  projectId: string;
  budgetCategoryId: string;
  supplierId: string | null;
  purchaseOrderId: string | null;
  expenseNumber: string;
  description: string;
  expenseDate: string;
  netAmount: number;
  taxAmount: number;
  totalAmount: number;
  status: ExpenseStatus;
  createdBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectionReason: string | null;
  voidReason: string | null;
  voidedBy: string | null;
  voidedAt: string | null;
  createdAt: string;
}

export type SupplierStatus =
  | "ACTIVE"
  | "INACTIVE"
  | "ARCHIVED";

export interface Supplier {
  id: string;
  name: string;
  contactName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  taxNumber: string | null;
  paymentTerms: string | null;
  notes: string | null;
  status: SupplierStatus;
  createdAt: string;
}

export type PurchaseOrderStatus =
  | "DRAFT"
  | "PENDING_APPROVAL"
  | "APPROVED"
  | "ORDERED"
  | "PARTIALLY_RECEIVED"
  | "RECEIVED"
  | "CANCELLED";

export interface PurchaseOrderItem {
  id: string;
  description: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  taxRate: number;
  lineSubtotal: number;
  taxAmount: number;
  lineTotal: number;
  quantityReceived: number;
  quantityRemaining: number;
}

export interface GoodsReceiptItem {
  purchaseOrderItemId: string;
  description: string;
  quantityReceived: number;
}

export interface GoodsReceipt {
  id: string;
  receivedDate: string;
  receivedByMembershipId: string;
  notes: string | null;
  createdAt: string;
  items: GoodsReceiptItem[];
}

export interface PurchaseOrderSummary {
  id: string;
  purchaseOrderNumber: string;
  supplierId: string;
  supplierName: string;
  status: PurchaseOrderStatus;
  issueDate: string;
  expectedDeliveryDate: string | null;
  subtotal: number;
  taxAmount: number;
  total: number;
  createdAt: string;
}

export interface PurchaseOrder {
  id: string;
  projectId: string;
  supplierId: string;
  supplierName: string;
  purchaseOrderNumber: string;
  status: PurchaseOrderStatus;
  issueDate: string;
  expectedDeliveryDate: string | null;
  subtotal: number;
  taxAmount: number;
  total: number;
  notes: string | null;
  createdBy: string;
  approvedBy: string | null;
  approvedAt: string | null;
  orderedAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdAt: string;
  items: PurchaseOrderItem[];
  receipts: GoodsReceipt[];
}
