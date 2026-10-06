"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Fragment, useEffect, useRef } from "react";

import { NotificationBell } from "@/components/NotificationBell";

type WorkspaceSection =
  | "overview"
  | "tasks"
  | "milestones"
  | "timeline"
  | "budget"
  | "expenses"
  | "procurement"
  | "commercial"
  | "changes"
  | "site"
  | "documents"
  | "photos"
  | "activity";

type NavItem = {
  key: WorkspaceSection;
  label: string;
  segment: string;
};

const NAV_GROUPS: NavItem[][] = [
  [{ key: "overview", label: "Overview", segment: "" }],
  [
    { key: "tasks", label: "Tasks", segment: "tasks" },
    { key: "milestones", label: "Milestones", segment: "milestones" },
    { key: "timeline", label: "Timeline", segment: "timeline" },
  ],
  [
    { key: "budget", label: "Budget", segment: "budget" },
    { key: "expenses", label: "Expenses", segment: "expenses" },
    { key: "procurement", label: "Procurement", segment: "procurement" },
    { key: "commercial", label: "Commercial", segment: "commercial" },
    { key: "changes", label: "Change Orders", segment: "change-orders" },
  ],
  [
    { key: "site", label: "Site", segment: "site" },
    { key: "documents", label: "Documents", segment: "documents" },
    { key: "photos", label: "Photos", segment: "photos" },
  ],
  [{ key: "activity", label: "Activity", segment: "activity" }],
];

function sectionFromPath(
  pathname: string | null,
  base: string,
): WorkspaceSection | undefined {
  if (!pathname || !pathname.startsWith(base)) {
    return undefined;
  }

  const segment =
    pathname.slice(base.length).split("/").filter(Boolean)[0] ?? "";

  return NAV_GROUPS.flat().find((item) => item.segment === segment)?.key;
}

export function ProjectWorkspaceNav({
  projectId,
  active,
}: {
  projectId: string;
  active?: WorkspaceSection;
}) {
  const pathname = usePathname();
  const base = `/app/projects/${projectId}`;
  const current = sectionFromPath(pathname, base) ?? active;
  const navRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const activeLink = navRef.current?.querySelector<HTMLElement>("a.active");

    activeLink?.scrollIntoView({ inline: "center", block: "nearest" });
  }, [current]);

  return (
    <div className="projectWorkspaceNavShell">
      <nav
        className="projectWorkspaceNav"
        aria-label="Project workspace"
        ref={navRef}
      >
        {NAV_GROUPS.map((group, index) => (
          <Fragment key={group[0].key}>
            {index > 0 && (
              <span className="projectWorkspaceNavDivider" aria-hidden="true" />
            )}

            {group.map((item) => (
              <Link
                key={item.key}
                className={current === item.key ? "active" : undefined}
                aria-current={current === item.key ? "page" : undefined}
                href={item.segment ? `${base}/${item.segment}` : base}
              >
                {item.label}
              </Link>
            ))}
          </Fragment>
        ))}
      </nav>

      <NotificationBell />
    </div>
  );
}
