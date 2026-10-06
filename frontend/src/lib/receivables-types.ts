export type AgingBucketKey =
  | "CURRENT"
  | "DAYS_1_30"
  | "DAYS_31_60"
  | "DAYS_61_90"
  | "DAYS_90_PLUS";

export interface AgingBucket {
  key: AgingBucketKey;
  label: string;
  invoices: number;
  amount: number;
}

export interface ExpectedCollection {
  /** Calendar month as "YYYY-MM". */
  month: string;
  invoices: number;
  amount: number;
}

export interface ProjectReceivable {
  projectId: string;
  projectNumber: string;
  projectName: string;
  openInvoices: number;
  outstanding: number;
  overdue: number;
  oldestOverdueDays: number;
}

export interface ReceivablesSummary {
  /** False for roles that may not see money owed (they get no data). */
  available: boolean;
  totalOutstanding: number;
  totalOverdue: number;
  openInvoices: number;
  overdueInvoices: number;
  overduePercentage: number;
  aging: AgingBucket[];
  expected: ExpectedCollection[];
  laterAmount: number;
  projects: ProjectReceivable[];
}
