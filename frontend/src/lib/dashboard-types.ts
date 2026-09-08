export interface DashboardStatusCount {
  status: string;
  count: number;
}

export interface DashboardMonthlyValue {
  month: string;
  value: number;
}

export interface DashboardBudgetPerformance {
  projectId: string;
  projectNumber: string;
  projectName: string;
  revisedBudget: number;
  forecastCost: number;
  variance: number;
  variancePercentage: number;
}

export interface DashboardDeadline {
  id: string;
  projectId: string;
  projectName: string;
  type: "TASK" | "MILESTONE";
  title: string;
  dueDate: string | null;
  priority: string | null;
}

export interface ProjectFinancialSnapshot {
  originalBudget: number;
  revisedBudget: number;
  actualCost: number;
  committedCost: number;
  forecastCost: number;
  budgetRemaining: number;
  forecastBudgetVariance: number;
  originalContractValue: number;
  approvedClientChangeOrders: number;
  revisedContractValue: number;
  totalInvoiced: number;
  totalPaid: number;
  outstandingReceivables: number;
  forecastProfit: number;
  forecastMarginPercentage: number;
}

export interface ProjectHealth {
  status:
    | "ON_TRACK"
    | "AT_RISK"
    | "CRITICAL"
    | "NOT_APPLICABLE";
  reasons: string[];
}

export interface OrganizationDashboard {
  activeProjects: number;
  totalContractValue: number;
  forecastRevenue: number;
  actualCosts: number;
  outstandingInvoices: number;
  projectsAtRisk: number;
  overdueTasks: number;
  openIncidents: number;
  currency: string;
  projectStatus: DashboardStatusCount[];
  monthlyRevenue: DashboardMonthlyValue[];
  monthlyExpenses: DashboardMonthlyValue[];
  budgetPerformance: DashboardBudgetPerformance[];
  upcomingDeadlines: DashboardDeadline[];
}

export interface ProjectDashboard {
  projectId: string;
  projectNumber: string;
  projectName: string;
  status: string;
  progressPercentage: number;
  startDate: string | null;
  expectedCompletion: string | null;
  projectManager: string | null;
  currency: string;
  health: ProjectHealth;
  financial: ProjectFinancialSnapshot;
  openTasks: number;
  overdueTasks: number;
  upcomingMilestones: number;
  openIncidents: number;
  outstandingInvoices: number;
  taskStatus: DashboardStatusCount[];
  monthlyExpenses: DashboardMonthlyValue[];
  costEvolution: DashboardMonthlyValue[];
  budgetUtilization: {
    originalBudget: number;
    revisedBudget: number;
    actualCost: number;
    forecastCost: number;
  };
  upcomingDeadlines: DashboardDeadline[];
}
