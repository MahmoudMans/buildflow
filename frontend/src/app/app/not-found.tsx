import Link from "next/link";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  EmptyState,
} from "@/components/ux/EmptyState";

export default function AppNotFound() {
  return (
    <main className="appShell">
      <AppSidebar active="overview" />

      <section className="appContent uxRouteState">
        <EmptyState
          title="This page was not found"
          description="The resource may have moved, been archived, or you may not have access to it."
          action={
            <Link
              className="uxPrimaryLink"
              href="/app"
            >
              Return to dashboard
            </Link>
          }
        />
      </section>
    </main>
  );
}
