import Link from "next/link";

export function ProjectWorkspaceNav({
  projectId,
  active,
}: {
  projectId: string;
  active:
    | "overview"
    | "tasks"
    | "milestones"
    | "budget"
    | "expenses"
    | "procurement";
}) {
  return (
    <nav className="projectWorkspaceNav">
      <Link
        className={
          active === "overview"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}`}
      >
        Overview
      </Link>

      <Link
        className={
          active === "tasks"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}/tasks`}
      >
        Tasks
      </Link>

      <Link
        className={
          active === "milestones"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}/milestones`}
      >
        Milestones
      </Link>

      <Link
        className={
          active === "budget"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}/budget`}
      >
        Budget
      </Link>

      <Link
        className={
          active === "expenses"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}/expenses`}
      >
        Expenses
      </Link>

      <Link
        className={
          active === "procurement"
            ? "active"
            : ""
        }
        href={`/app/projects/${projectId}/procurement`}
      >
        Procurement
      </Link>
    </nav>
  );
}
