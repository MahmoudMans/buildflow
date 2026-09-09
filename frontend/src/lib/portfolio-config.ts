export const portfolioConfig = {
  githubUrl:
    "https://github.com/MahmoudMans/buildflow",
  demoPassword:
    "BuildFlowDemo2026!",
  demoOrganization:
    "NorthBuild Construction",
} as const;

export const demoAccounts = [
  {
    key: "owner",
    label: "Owner",
    email:
      "owner@buildflow.demo",
    description:
      "Full company, project, finance and administrative access.",
    destination:
      "/app",
  },
  {
    key: "project-manager",
    label: "Project Manager",
    email:
      "pm@buildflow.demo",
    description:
      "Assigned-project planning, execution and financial visibility.",
    destination:
      "/app",
  },
  {
    key: "site-manager",
    label: "Site Manager",
    email:
      "site@buildflow.demo",
    description:
      "Mobile-oriented site operations, reports, incidents and progress.",
    destination:
      "/app",
  },
  {
    key: "accountant",
    label: "Accountant",
    email:
      "accountant@buildflow.demo",
    description:
      "Invoices, payments, expenses and financial reporting.",
    destination:
      "/app",
  },
  {
    key: "client",
    label: "Client",
    email:
      "client@buildflow.demo",
    description:
      "Restricted client portal with explicitly shared project information.",
    destination:
      "/app/client",
  },
] as const;

export type DemoRole =
  typeof demoAccounts[number]["key"];
