"use client";

import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  ErrorState,
} from "@/components/ux/ErrorState";

export default function AppError({
  error,
  reset,
}: {
  error: Error & {
    digest?: string;
  };
  reset: () => void;
}) {
  return (
    <main className="appShell">
      <AppSidebar active="overview" />

      <section className="appContent uxRouteState">
        <ErrorState
          title="Unable to load this workspace"
          message={
            error.message
            || "BuildFlow could not load this screen."
          }
          onRetry={reset}
        />
      </section>
    </main>
  );
}
