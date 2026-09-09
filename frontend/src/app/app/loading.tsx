import {
  AppSidebar,
} from "@/components/AppSidebar";

import {
  LoadingSkeleton,
} from "@/components/ux/LoadingSkeleton";

export default function AppLoading() {
  return (
    <main
      className="appShell"
      aria-busy="true"
    >
      <AppSidebar active="overview" />

      <section className="appContent uxRouteState">
        <LoadingSkeleton
          cards={4}
          lines={5}
          label="Loading BuildFlow workspace"
        />
      </section>
    </main>
  );
}
