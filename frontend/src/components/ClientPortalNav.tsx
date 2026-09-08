import Link from "next/link";

export function ClientPortalNav({
  projectId,
}: {
  projectId?: string;
}) {
  return (
    <nav className="clientPortalNav">
      <Link href="/app/client">
        My Projects
      </Link>

      {projectId && (
        <>
          <a href="#overview">
            Overview
          </a>

          <a href="#progress">
            Progress
          </a>

          <a href="#milestones">
            Milestones
          </a>

          <a href="#photos">
            Photos
          </a>

          <a href="#documents">
            Documents
          </a>

          <a href="#changes">
            Change Orders
          </a>

          <a href="#quotes">
            Quotes
          </a>

          <a href="#invoices">
            Invoices
          </a>
        </>
      )}
    </nav>
  );
}
