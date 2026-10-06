"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

type SidebarSection =
  | "overview"
  | "projects"
  | "clients"
  | "suppliers"
  | "team"
  | "reports"
  | "notifications";

const NAV_ITEMS: { key: SidebarSection; label: string; href: string }[] = [
  { key: "overview", label: "Overview", href: "/app" },
  { key: "projects", label: "Projects", href: "/app/projects" },
  { key: "clients", label: "Clients", href: "/app/clients" },
  { key: "suppliers", label: "Suppliers", href: "/app/suppliers" },
  { key: "team", label: "Team", href: "/app/team" },
  { key: "reports", label: "Reports", href: "/app/reports" },
  { key: "notifications", label: "Notifications", href: "/app/notifications" },
];

function sectionFromPath(pathname: string | null): SidebarSection | undefined {
  if (!pathname) {
    return undefined;
  }

  if (pathname === "/app") {
    return "overview";
  }

  return NAV_ITEMS.find(
    (item) =>
      item.href !== "/app" &&
      (pathname === item.href || pathname.startsWith(`${item.href}/`)),
  )?.key;
}

export function AppSidebar({ active }: { active?: SidebarSection }) {
  const pathname = usePathname();
  const current = sectionFromPath(pathname) ?? active;

  return (
    <aside className="appSidebar">
      <Link className="sidebarBrand" href="/app">
        <span>B</span>
        BuildFlow
      </Link>

      <nav aria-label="Primary">
        {NAV_ITEMS.map((item) => (
          <Link
            key={item.key}
            className={current === item.key ? "active" : undefined}
            aria-current={current === item.key ? "page" : undefined}
            href={item.href}
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  );
}
