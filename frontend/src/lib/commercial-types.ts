export type QuoteStatus =
  | "DRAFT"
  | "SENT"
  | "VIEWED"
  | "ACCEPTED"
  | "REJECTED"
  | "EXPIRED"
  | "CANCELLED";

export type InvoiceStatus =
  | "DRAFT"
  | "ISSUED"
  | "PARTIALLY_PAID"
  | "PAID"
  | "OVERDUE"
  | "VOID";

export type PaymentMethod =
  | "BANK_TRANSFER"
  | "CASH"
  | "CARD"
  | "CHEQUE"
  | "OTHER";

export interface QuoteLine {
  id: string;
  description: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  taxRate: number;
  lineSubtotal: number;
  taxAmount: number;
  lineTotal: number;
}

export interface QuoteSummary {
  id: string;
  quoteNumber: string;
  status: QuoteStatus;
  contractQuote: boolean;
  issueDate: string;
  validUntil: string | null;
  total: number;
  createdAt: string;
}

export interface Quote {
  id: string;
  projectId: string;
  clientId: string;
  quoteNumber: string;
  status: QuoteStatus;
  contractQuote: boolean;
  issueDate: string;
  validUntil: string | null;
  subtotal: number;
  taxAmount: number;
  total: number;
  notes: string | null;
  terms: string | null;
  acceptedAt: string | null;
  items: QuoteLine[];
}

export interface InvoiceLine {
  id: string;
  description: string;
  quantity: number;
  unit: string;
  unitPrice: number;
  taxRate: number;
  lineSubtotal: number;
  taxAmount: number;
  lineTotal: number;
}

export interface Payment {
  id: string;
  paymentDate: string;
  amount: number;
  method: PaymentMethod;
  reference: string | null;
  notes: string | null;
  createdAt: string;
}

export interface InvoiceSummary {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  issueDate: string;
  dueDate: string;
  total: number;
  amountPaid: number;
  amountOutstanding: number;
  createdAt: string;
}

export interface Invoice {
  id: string;
  projectId: string;
  clientId: string;
  sourceQuoteId: string | null;
  invoiceNumber: string;
  status: InvoiceStatus;
  issueDate: string;
  dueDate: string;
  subtotal: number;
  taxAmount: number;
  total: number;
  amountPaid: number;
  amountOutstanding: number;
  notes: string | null;
  paymentInstructions: string | null;
  issuedAt: string | null;
  voidedAt: string | null;
  voidReason: string | null;
  createdAt: string;
  items: InvoiceLine[];
  payments: Payment[];
}

export interface RevenueSummary {
  projectId: string;
  originalContractValue: number;
  approvedClientChangeOrders: number;
  revisedContractValue: number;
  totalInvoiced: number;
  totalPaid: number;
  outstandingReceivables: number;
  forecastCost: number;
  forecastProfit: number;
  forecastMarginPercentage: number;
}
