export interface ClientPortalProjectSummary {
  id: string;
  projectNumber: string;
  name: string;
  status: string;
  progressPercentage: number;
  plannedEndDate: string | null;
}

export interface ClientPortalMilestone {
  id: string;
  name: string;
  plannedDate: string | null;
  status: string;
}

export interface ClientPortalProgressUpdate {
  id: string;
  progressPercentage: number;
  summary: string;
  nextSteps: string | null;
  createdAt: string;
}

export interface ClientPortalChangeOrder {
  id: string;
  changeOrderNumber: string;
  title: string;
  description: string | null;
  reason: string | null;
  clientPriceImpact: number;
  scheduleImpactDays: number | null;
  status: string;
  updatedAt: string;
}

export interface ClientPortalQuote {
  id: string;
  quoteNumber: string;
  status: string;
  issueDate: string | null;
  validUntil: string | null;
  total: number;
}

export interface ClientPortalInvoice {
  id: string;
  invoiceNumber: string;
  status: string;
  issueDate: string | null;
  dueDate: string | null;
  total: number;
  amountPaid: number;
}

export interface ClientPortalProjectView {
  project: ClientPortalProjectSummary;
  upcomingMilestones: ClientPortalMilestone[];
  recentUpdates: ClientPortalProgressUpdate[];
  changeOrders: ClientPortalChangeOrder[];
  quotes: ClientPortalQuote[];
  invoices: ClientPortalInvoice[];
}
