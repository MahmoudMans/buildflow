import Link from "next/link";

export function ProjectWorkspaceNav({
  projectId,
  active,
}: {
  projectId: string;
  active:
    | "overview"
    | "tasks"
    | "milestones";
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
    </nav>
  );
}
