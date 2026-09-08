"use client";

import Link from "next/link";

export function AppSidebar({
  active,
}: {
  active:
    | "overview"
    | "clients"
    | "projects"
    | "team";
}) {
  return (
    <aside className="appSidebar">
      <Link
        className="sidebarBrand"
        href="/app"
      >
        <span>B</span>
        BuildFlow
      </Link>

      <nav>
        <Link
          className={
            active === "overview"
              ? "active"
              : ""
          }
          href="/app"
        >
          Overview
        </Link>

        <Link
          className={
            active === "projects"
              ? "active"
              : ""
          }
          href="/app/projects"
        >
          Projects
        </Link>

        <Link
          className={
            active === "clients"
              ? "active"
              : ""
          }
          href="/app/clients"
        >
          Clients
        </Link>

        <Link
          className={
            active === "team"
              ? "active"
              : ""
          }
          href="/app/team"
        >
          Team
        </Link>
      </nav>
    </aside>
  );
}
