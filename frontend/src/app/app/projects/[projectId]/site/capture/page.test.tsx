import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import type { DailySiteReport } from "@/lib/site-operation-types";

import SiteCapturePage from "./page";

const PROJECT = "22222222-0000-0000-0000-000000000002";
const TODAY = "2026-10-06";

const mocks = vi.hoisted(() => {
  class ApiClientError extends Error {
    status: number;
    code: string;

    constructor(status: number, code: string, message: string) {
      super(message);
      this.status = status;
      this.code = code;
    }
  }

  return {
    ApiClientError,
    replace: vi.fn(),
    apiGet: vi.fn(),
    apiPost: vi.fn(),
    prepare: vi.fn(),
    auth: {
      user: { id: "u1" } as { id: string } | null,
      loading: false,
      organizations: [
        {
          id: "org-1",
          name: "Acme Build",
          currency: "EUR",
          timezone: "Africa/Tunis",
          role: "SITE_MANAGER",
        },
      ],
    },
  };
});

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: mocks.replace }),
  useParams: () => ({ projectId: PROJECT }),
  usePathname: () => `/app/projects/${PROJECT}/site/capture`,
}));

vi.mock("@/components/AuthProvider", () => ({
  useAuth: () => mocks.auth,
}));

vi.mock("@/components/AppSidebar", () => ({
  AppSidebar: () => null,
}));

vi.mock("@/lib/api", () => ({
  ApiClientError: mocks.ApiClientError,
  apiGet: mocks.apiGet,
  apiPost: mocks.apiPost,
}));

vi.mock("@/lib/site-photo", () => ({
  prepareBrowserPhoto: mocks.prepare,
}));

function report(change: Partial<DailySiteReport> = {}): DailySiteReport {
  return {
    id: "r9",
    projectId: PROJECT,
    reportDate: TODAY,
    weatherText: "Cloudy",
    workersOnSite: 8,
    summary: "Earlier summary",
    workCompleted: null,
    delays: null,
    issues: null,
    nextSteps: null,
    createdBy: "u1",
    createdAt: "2026-10-06T08:00:00Z",
    updatedAt: "2026-10-06T08:00:00Z",
    ...change,
  };
}

function serve(reports: DailySiteReport[] = []) {
  mocks.apiGet.mockImplementation(async (path: string) => {
    if (path === `/projects/${PROJECT}`) {
      return { name: "Harbor Office" };
    }

    if (path === `/projects/${PROJECT}/site-reports`) {
      return reports;
    }

    throw new Error(`unexpected GET ${path}`);
  });

  mocks.apiPost.mockImplementation(async (path: string, body: unknown) => {
    if (path === `/projects/${PROJECT}/site-reports`) {
      return { ...(body as object), id: "r1" };
    }

    if (path.endsWith("/update")) {
      return { id: path.split("/")[2] };
    }

    if (path.endsWith("/photos")) {
      return { id: "p1" };
    }

    throw new Error(`unexpected POST ${path}`);
  });
}

function posts(suffix: string): unknown[][] {
  return mocks.apiPost.mock.calls.filter((call: unknown[]) => String(call[0]).endsWith(suffix));
}

async function openPage() {
  render(<SiteCapturePage />);

  await screen.findByText(/Step 1 of 4/);
}

function press(name: string) {
  fireEvent.click(screen.getByRole("button", { name }));
}

function heading() {
  return screen.getByRole("heading", { level: 2 }).textContent;
}

function writeSummary(text: string) {
  fireEvent.change(screen.getByLabelText(/^Summary/), { target: { value: text } });
}

// Basics -> Work (summary written) -> Photos.
async function reachPhotos(summary = "Slab poured on level 2") {
  press("Next");
  await screen.findByRole("heading", { name: "What happened today" });
  writeSummary(summary);
  press("Next");
  await screen.findByRole("heading", { name: "Add photos" });
}

beforeEach(() => {
  window.localStorage.clear();
  vi.useFakeTimers({ toFake: ["Date"] });
  vi.setSystemTime(new Date("2026-10-06T10:00:00Z"));
  mocks.auth.user = { id: "u1" };
  mocks.auth.loading = false;
  mocks.auth.organizations[0].role = "SITE_MANAGER";
  mocks.prepare.mockImplementation(async (file: File) => ({
    fileName: file.name,
    mimeType: "image/jpeg",
    dataUrl: "data:image/jpeg;base64,AAAA",
    sizeBytes: 1234,
    width: null,
    height: null,
    resized: false,
  }));
  serve();
});

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.restoreAllMocks();
  mocks.apiGet.mockReset();
  mocks.apiPost.mockReset();
  mocks.prepare.mockReset();
  mocks.replace.mockReset();
});

describe("Quick site report page", () => {
  it("starts on the first step with the project and today's date", async () => {
    await openPage();

    expect(screen.getByText("Step 1 of 4 · Harbor Office")).toBeTruthy();
    expect(heading()).toBe("The day on site");
    expect((screen.getByLabelText("Report date") as HTMLInputElement).value).toBe(TODAY);
  });

  it("uses the date where the organization is, not the UTC date", async () => {
    vi.setSystemTime(new Date("2026-10-05T23:30:00Z"));

    await openPage();

    expect((screen.getByLabelText("Report date") as HTMLInputElement).value).toBe("2026-10-06");
  });

  it("is not offered to people who cannot add reports", async () => {
    mocks.auth.organizations[0].role = "CLIENT";

    render(<SiteCapturePage />);

    expect(
      await screen.findByText("Site reports are added by site and project managers"),
    ).toBeTruthy();
    expect(mocks.apiGet).not.toHaveBeenCalled();
  });

  it("asks for a summary before moving on", async () => {
    await openPage();

    press("Next");
    await screen.findByRole("heading", { name: "What happened today" });

    press("Next");

    expect(await screen.findByText("Write a short summary of the day.")).toBeTruthy();
    expect(heading()).toBe("What happened today");

    writeSummary("Done");
    press("Next");

    expect(await screen.findByRole("heading", { name: "Add photos" })).toBeTruthy();
  });

  it("creates the report and says it was sent", async () => {
    await openPage();

    fireEvent.click(screen.getByRole("button", { name: "Sunny" }));
    fireEvent.change(screen.getByLabelText("Workers on site"), { target: { value: "14" } });

    await reachPhotos("  Slab poured on level 2  ");

    press("Skip photos");
    await screen.findByRole("heading", { name: "Check and send" });

    press("Send report");

    expect(await screen.findByRole("heading", { name: "Report sent" })).toBeTruthy();

    const created = posts("/site-reports");

    expect(created).toHaveLength(1);
    expect(created[0][0]).toBe(`/projects/${PROJECT}/site-reports`);
    expect(created[0][1]).toEqual({
      reportDate: TODAY,
      weatherText: "Sunny",
      workersOnSite: 14,
      summary: "Slab poured on level 2",
      workCompleted: null,
      delays: null,
      issues: null,
      nextSteps: null,
    });
    expect(created[0][2]).toBe("org-1");
  });

  it("updates the report that already exists for the day", async () => {
    serve([report()]);

    await openPage();

    expect(
      screen.getByText(/already exists\. Sending will update it and add any photos\./),
    ).toBeTruthy();
    expect((screen.getByLabelText("Workers on site") as HTMLInputElement).value).toBe("8");

    press("Next");
    await screen.findByRole("heading", { name: "What happened today" });

    expect((screen.getByLabelText(/^Summary/) as HTMLTextAreaElement).value).toBe("Earlier summary");

    writeSummary("Updated summary");
    press("Next");
    await screen.findByRole("heading", { name: "Add photos" });
    press("Skip photos");
    await screen.findByRole("heading", { name: "Check and send" });
    press("Send report");

    await screen.findByRole("heading", { name: "Report sent" });

    expect(posts("/site-reports")).toHaveLength(0);

    const updated = posts("/update");

    expect(updated).toHaveLength(1);
    expect(updated[0][0]).toBe("/site-reports/r9/update");
    expect((updated[0][1] as { summary: string }).summary).toBe("Updated summary");
  });

  it("sends the photos and tries only the failed ones again", async () => {
    let photoCalls = 0;

    mocks.apiPost.mockImplementation(async (path: string, body: unknown) => {
      if (path === `/projects/${PROJECT}/site-reports`) {
        return { ...(body as object), id: "r1" };
      }

      if (path === "/site-reports/r1/photos") {
        photoCalls += 1;

        if (photoCalls === 1) {
          throw new TypeError("Failed to fetch");
        }

        return { id: `p${photoCalls}` };
      }

      throw new Error(`unexpected POST ${path}`);
    });

    await openPage();
    await reachPhotos();

    fireEvent.change(screen.getByLabelText("Choose from gallery"), {
      target: { files: [new File(["x"], "wall.jpg", { type: "image/jpeg" })] },
    });

    expect(await screen.findByRole("img", { name: "Photo 1" })).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Caption"), { target: { value: "North wall" } });
    fireEvent.change(screen.getByLabelText("Category"), { target: { value: "AFTER" } });

    press("Next");
    await screen.findByRole("heading", { name: "Check and send" });
    press("Send report");

    expect(
      await screen.findByRole("heading", { name: "The report is saved, but some photos did not send" }),
    ).toBeTruthy();
    expect(
      screen.getByText("There is no connection right now. Check your signal and try again."),
    ).toBeTruthy();

    press("Try sending again");

    expect(await screen.findByRole("heading", { name: "Report sent" })).toBeTruthy();

    expect(posts("/site-reports")).toHaveLength(1);

    const photos = posts("/photos");

    expect(photos).toHaveLength(2);
    expect(photos[1][0]).toBe("/site-reports/r1/photos");
    expect(photos[1][1]).toEqual({
      fileName: "wall.jpg",
      mimeType: "image/jpeg",
      base64Data: "data:image/jpeg;base64,AAAA",
      caption: "North wall",
      category: "AFTER",
      clientVisible: false,
    });
  });

  it("shows why a photo could not be added and carries on", async () => {
    mocks.prepare.mockRejectedValue(new Error("This photo could not be opened."));

    await openPage();
    await reachPhotos();

    fireEvent.change(screen.getByLabelText("Choose from gallery"), {
      target: { files: [new File(["x"], "scan.heic", { type: "image/heic" })] },
    });

    expect(await screen.findByText(/scan\.heic: This photo could not be opened\./)).toBeTruthy();
    expect(screen.queryByRole("img", { name: "Photo 1" })).toBeNull();
  });

  it("keeps a draft on the phone and offers it again", async () => {
    await openPage();

    press("Next");
    await screen.findByRole("heading", { name: "What happened today" });
    writeSummary("Half-written summary");

    await waitFor(
      () => {
        const stored = window.localStorage.getItem(
          `buildflow.siteReportDraft.org-1.${PROJECT}.${TODAY}`,
        );

        expect(stored).toContain("Half-written summary");
      },
      { timeout: 3000 },
    );

    cleanup();

    await openPage();

    expect(await screen.findByText(/Restored your unsent draft/)).toBeTruthy();

    press("Next");
    await screen.findByRole("heading", { name: "What happened today" });

    expect((screen.getByLabelText(/^Summary/) as HTMLTextAreaElement).value).toBe(
      "Half-written summary",
    );
  });

  it("shows the refusal and keeps everything when the server says no", async () => {
    mocks.apiPost.mockRejectedValue(
      new mocks.ApiClientError(
        403,
        "SITE_OPERATIONS_MANAGE_FORBIDDEN",
        "You do not have permission to manage site operations for this project.",
      ),
    );

    await openPage();
    await reachPhotos();
    press("Skip photos");
    await screen.findByRole("heading", { name: "Check and send" });
    press("Send report");

    expect(
      await screen.findByText("You do not have permission to manage site operations for this project."),
    ).toBeTruthy();
    expect(heading()).toBe("Check and send");
    expect((screen.getByRole("button", { name: "Send report" }) as HTMLButtonElement).disabled).toBe(
      false,
    );
  });

  it("will not send while the phone is offline", async () => {
    vi.spyOn(window.navigator, "onLine", "get").mockReturnValue(false);

    await openPage();

    expect(screen.getByText(/You are offline\. What you type is kept on this phone/)).toBeTruthy();

    await reachPhotos();
    press("Skip photos");
    await screen.findByRole("heading", { name: "Check and send" });

    expect((screen.getByRole("button", { name: "Send report" }) as HTMLButtonElement).disabled).toBe(
      true,
    );
    expect(screen.getByText("You are offline, so the report cannot be sent yet.")).toBeTruthy();
  });
});
