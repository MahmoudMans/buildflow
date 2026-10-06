"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";

import { useAuth } from "@/components/AuthProvider";
import { useOrganizationSelection } from "@/components/useOrganizationSelection";
import { openSearchPalette } from "@/lib/search";

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
  const { organizations, logout } = useAuth();
  const selection = useOrganizationSelection(organizations);
  const [signingOut, setSigningOut] = useState(false);
  const current = sectionFromPath(pathname) ?? active;

  async function handleSignOut() {
    if (signingOut) {
      return;
    }

    setSigningOut(true);

    try {
      await logout();
    } catch {
      // The session is cleared locally even if the request fails.
    } finally {
      setSigningOut(false);
    }
  }

  return (
    <>
      <aside className="appSidebar">
        <Link className="sidebarBrand" href="/app">
          <span>B</span>
          BuildFlow
        </Link>

        {organizations.length > 0 && (
          <div className="sidebarOrg">
            <span className="sidebarOrgLabel" id="sidebar-org-label">
              Organization
            </span>

            {organizations.length > 1 ? (
              <select
                aria-labelledby="sidebar-org-label"
                value={selection.organizationId}
                onChange={(event) =>
                  selection.selectOrganization(event.target.value)
                }
              >
                {organizations.map((organization) => (
                  <option key={organization.id} value={organization.id}>
                    {organization.name}
                  </option>
                ))}
              </select>
            ) : (
              <strong className="sidebarOrgName">{organizations[0].name}</strong>
            )}
          </div>
        )}

        <button
          type="button"
          className="sidebarSearch"
          onClick={openSearchPalette}
          aria-label="Search (Command or Control plus K)"
          aria-keyshortcuts="Meta+K Control+K"
        >
          <span>Search</span>
          <kbd aria-hidden="true">⌘K</kbd>
        </button>

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

        <div className="sidebarFooter">
          <button
            type="button"
            className="sidebarSignOut"
            onClick={handleSignOut}
            disabled={signingOut}
          >
            {signingOut ? "Signing out..." : "Sign out"}
          </button>
        </div>
      </aside>

      <nav className="appMobileNav" aria-label="Mobile navigation">
        <button type="button" onClick={openSearchPalette}>
          Search
        </button>

        {organizations.length > 1 && (
          <select
            aria-label="Organization"
            value={selection.organizationId}
            onChange={(event) =>
              selection.selectOrganization(event.target.value)
            }
          >
            {organizations.map((organization) => (
              <option key={organization.id} value={organization.id}>
                {organization.name}
              </option>
            ))}
          </select>
        )}

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

        <button type="button" onClick={handleSignOut} disabled={signingOut}>
          {signingOut ? "Signing out..." : "Sign out"}
        </button>
      </nav>
    </>
  );
}
