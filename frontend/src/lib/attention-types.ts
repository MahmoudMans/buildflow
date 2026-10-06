export type AttentionSeverity = "CRITICAL" | "WARNING" | "INFO";

export type AttentionKind =
  | "PROJECT_AT_RISK"
  | "OVERDUE_TASK"
  | "OPEN_INCIDENT"
  | "OVERDUE_INVOICE"
  | "CHANGE_ORDER_REVIEW";

export interface AttentionItem {
  kind: AttentionKind;
  severity: AttentionSeverity;
  entityId: string;
  projectId: string;
  projectNumber: string;
  projectName: string;
  title: string;
  detail: string;
  date: string | null;
  amount: number | null;
}

export interface AttentionSummary {
  financeVisible: boolean;
  overdueTasks: number;
  openIncidents: number;
  overdueInvoices: number;
  overdueInvoiceAmount: number;
  changeOrdersAwaitingClient: number;
  projectsAtRisk: number;
  items: AttentionItem[];
}
