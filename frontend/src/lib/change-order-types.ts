export type ChangeOrderStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "CLIENT_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED";

export interface ChangeOrderSummary {
  id: string;
  changeOrderNumber: string;
  title: string;
  status: ChangeOrderStatus;
  costImpact: number | null;
  clientPriceImpact: number;
  scheduleImpactDays: number;
  createdAt: string;
}

export interface ChangeOrder {
  id: string;
  projectId: string;
  changeOrderNumber: string;
  title: string;
  description: string | null;
  reason: string | null;
  costImpact: number | null;
  clientPriceImpact: number;
  scheduleImpactDays: number;
  status: ChangeOrderStatus;
  requestedBy: string;
  submittedAt: string | null;
  clientReviewAt: string | null;
  approvedBy: string | null;
  approvedAt: string | null;
  rejectedBy: string | null;
  rejectedAt: string | null;
  cancelledBy: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeOrderFinancialSummary {
  projectId: string;
  originalBudget: number;
  approvedBudgetChangeOrders: number;
  revisedBudget: number;
  originalContractValue: number;
  approvedClientChangeOrders: number;
  revisedContractValue: number;
  forecastCost: number;
  forecastProfit: number;
  forecastMarginPercentage: number;
}
