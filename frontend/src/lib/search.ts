import { humanizeStatus } from "./ux-validation";

export type SearchKind =
  | "PROJECT"
  | "TASK"
  | "CLIENT"
  | "SUPPLIER"
  | "INVOICE"
  | "PURCHASE_ORDER";

export interface SearchHit {
  kind: SearchKind;
  id: string;
  /** Owning project; null for clients and suppliers. */
  projectId: string | null;
  title: string;
  subtitle: string;
  status: string | null;
}

export interface SearchResponse {
  query: string;
  hits: SearchHit[];
}

/** Dispatched on window to open the command palette from anywhere. */
export const OPEN_SEARCH_EVENT = "buildflow:open-search";

export function openSearchPalette() {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(OPEN_SEARCH_EVENT));
  }
}

export const MIN_SEARCH_LENGTH = 2;

export const SEARCH_KIND_ORDER: readonly SearchKind[] = [
  "PROJECT",
  "TASK",
  "CLIENT",
  "SUPPLIER",
  "INVOICE",
  "PURCHASE_ORDER",
];

export const SEARCH_KIND_LABELS: Record<SearchKind, string> = {
  PROJECT: "Projects",
  TASK: "Tasks",
  CLIENT: "Clients",
  SUPPLIER: "Suppliers",
  INVOICE: "Invoices",
  PURCHASE_ORDER: "Purchase orders",
};

export function hitHref(hit: SearchHit): string {
  const project = hit.projectId;

  switch (hit.kind) {
    case "PROJECT":
      return `/app/projects/${hit.id}`;
    case "TASK":
      return project
        ? `/app/projects/${project}/tasks/${hit.id}`
        : "/app/projects";
    case "CLIENT":
      return `/app/clients/${hit.id}`;
    case "SUPPLIER":
      return "/app/suppliers";
    case "INVOICE":
      return project
        ? `/app/projects/${project}/commercial`
        : "/app/projects";
    case "PURCHASE_ORDER":
      return project
        ? `/app/projects/${project}/procurement`
        : "/app/projects";
  }
}

export function statusLabel(status: string | null): string {
  return status ? humanizeStatus(status) : "";
}

export interface QuickLink {
  id: string;
  label: string;
  hint: string;
  href: string;
}

export const QUICK_LINKS: readonly QuickLink[] = [
  { id: "overview", label: "Overview", hint: "Dashboard", href: "/app" },
  {
    id: "projects",
    label: "Projects",
    hint: "All projects",
    href: "/app/projects",
  },
  { id: "clients", label: "Clients", hint: "Client list", href: "/app/clients" },
  {
    id: "suppliers",
    label: "Suppliers",
    hint: "Supplier list",
    href: "/app/suppliers",
  },
  { id: "team", label: "Team", hint: "Members and roles", href: "/app/team" },
  {
    id: "reports",
    label: "Reports",
    hint: "Exports and reports",
    href: "/app/reports",
  },
  {
    id: "notifications",
    label: "Notifications",
    hint: "Your inbox",
    href: "/app/notifications",
  },
];

export function filterQuickLinks(query: string): QuickLink[] {
  const needle = query.trim().toLowerCase();

  if (!needle) {
    return [...QUICK_LINKS];
  }

  return QUICK_LINKS.filter((link) =>
    link.label.toLowerCase().includes(needle),
  );
}

export interface PaletteItem {
  key: string;
  href: string;
  title: string;
  subtitle: string;
  status: string | null;
}

export interface PaletteSection {
  id: string;
  label: string;
  items: PaletteItem[];
}

/**
 * Pages whose name matches come first, then the search results grouped by
 * kind in a fixed order. Empty groups are left out.
 */
export function buildSections(
  query: string,
  hits: readonly SearchHit[],
): PaletteSection[] {
  const sections: PaletteSection[] = [];

  const links = filterQuickLinks(query);

  if (links.length > 0) {
    sections.push({
      id: "pages",
      label: "Go to",
      items: links.map((link) => ({
        key: `page:${link.id}`,
        href: link.href,
        title: link.label,
        subtitle: link.hint,
        status: null,
      })),
    });
  }

  for (const kind of SEARCH_KIND_ORDER) {
    const group = hits.filter((hit) => hit.kind === kind);

    if (group.length === 0) {
      continue;
    }

    sections.push({
      id: kind,
      label: SEARCH_KIND_LABELS[kind],
      items: group.map((hit) => ({
        key: `${hit.kind}:${hit.id}`,
        href: hitHref(hit),
        title: hit.title,
        subtitle: hit.subtitle,
        status: hit.kind === "PROJECT" ? hit.status : null,
      })),
    });
  }

  return sections;
}
