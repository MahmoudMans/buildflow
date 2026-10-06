import { describe, expect, it } from "vitest";

import type { DailySiteReport } from "@/lib/site-operation-types";

import {
  CAPTURE_STEPS,
  PHOTO_CATEGORIES,
  buildCreateRequest,
  buildPhotoRequest,
  buildUpdateRequest,
  canCaptureSiteReport,
  draftFromReport,
  draftKey,
  emptyDraft,
  errorMessage,
  errorsForStep,
  firstStepWithError,
  formatReportDay,
  formatSavedTime,
  formatSize,
  isDraftEmpty,
  isValidDay,
  nextStep,
  parseWorkers,
  pendingPhotos,
  photoCategoryLabel,
  photoSummary,
  previousStep,
  readDraft,
  removeDraft,
  stepForField,
  stepIndex,
  todayInTimeZone,
  uploadPending,
  validateDraft,
  writeDraft,
} from "./site-capture";
import type { CaptureDraft, CapturePhoto, PhotoStatus, StorageLike } from "./site-capture";

function draft(change: Partial<CaptureDraft> = {}): CaptureDraft {
  return {
    ...emptyDraft("2026-10-06"),
    summary: "Slab poured on level 2",
    ...change,
  };
}

function photo(id: string, change: Partial<CapturePhoto> = {}): CapturePhoto {
  return {
    id,
    fileName: `${id}.jpg`,
    mimeType: "image/jpeg",
    dataUrl: `data:image/jpeg;base64,${id}`,
    sizeBytes: 250_000,
    category: "PROGRESS",
    caption: "",
    clientVisible: false,
    status: "ready",
    error: "",
    ...change,
  };
}

function memoryStorage(): StorageLike & { data: Record<string, string> } {
  const data: Record<string, string> = {};

  return {
    data,
    getItem: (key) => (key in data ? data[key] : null),
    setItem: (key, value) => {
      data[key] = value;
    },
    removeItem: (key) => {
      delete data[key];
    },
  };
}

describe("canCaptureSiteReport", () => {
  it("allows the roles that manage site operations", () => {
    expect(canCaptureSiteReport("OWNER")).toBe(true);
    expect(canCaptureSiteReport("ADMIN")).toBe(true);
    expect(canCaptureSiteReport("PROJECT_MANAGER")).toBe(true);
    expect(canCaptureSiteReport("SITE_MANAGER")).toBe(true);
  });

  it("refuses everyone else", () => {
    expect(canCaptureSiteReport("CLIENT")).toBe(false);
    expect(canCaptureSiteReport("ACCOUNTANT")).toBe(false);
    expect(canCaptureSiteReport(undefined)).toBe(false);
    expect(canCaptureSiteReport(null)).toBe(false);
  });
});

describe("steps", () => {
  it("lists four steps in order", () => {
    expect(CAPTURE_STEPS.map((step) => step.key)).toEqual(["basics", "work", "photos", "review"]);
  });

  it("moves forward and back without leaving the list", () => {
    expect(nextStep("basics")).toBe("work");
    expect(nextStep("photos")).toBe("review");
    expect(nextStep("review")).toBe("review");
    expect(previousStep("review")).toBe("photos");
    expect(previousStep("basics")).toBe("basics");
    expect(stepIndex("photos")).toBe(2);
  });
});

describe("dates", () => {
  it("knows a real calendar day", () => {
    expect(isValidDay("2026-10-06")).toBe(true);
    expect(isValidDay("2028-02-29")).toBe(true);
    expect(isValidDay("2026-02-29")).toBe(false);
    expect(isValidDay("2026-13-01")).toBe(false);
    expect(isValidDay("06/10/2026")).toBe(false);
    expect(isValidDay("")).toBe(false);
  });

  it("uses the date where the organization is, not the UTC date", () => {
    const lateInUtc = new Date("2026-10-05T23:30:00Z");

    expect(todayInTimeZone(lateInUtc, "Africa/Tunis")).toBe("2026-10-06");
    expect(todayInTimeZone(lateInUtc, "UTC")).toBe("2026-10-05");
    expect(todayInTimeZone(lateInUtc, "America/Los_Angeles")).toBe("2026-10-05");
  });

  it("falls back to the UTC date for an unknown time zone", () => {
    expect(todayInTimeZone(new Date("2026-10-05T23:30:00Z"), "Mars/Base")).toBe("2026-10-05");
  });

  it("writes a day in words", () => {
    expect(formatReportDay("2026-10-06")).toBe("Tue 6 Oct 2026");
    expect(formatReportDay("2026-01-01")).toBe("Thu 1 Jan 2026");
    expect(formatReportDay("nonsense")).toBe("nonsense");
  });

  it("shows the saved time in the organization's time zone", () => {
    expect(formatSavedTime("2026-10-06T12:32:00Z", "Africa/Tunis")).toBe("13:32");
    expect(formatSavedTime("2026-10-06T12:32:00Z", "UTC")).toBe("12:32");
    expect(formatSavedTime("2026-10-06T12:32:00Z", "Mars/Base")).toBe("12:32");
    expect(formatSavedTime("not a time", "UTC")).toBe("");
  });
});

describe("the draft", () => {
  it("starts empty on the chosen day", () => {
    const empty = emptyDraft("2026-10-06");

    expect(empty.reportDate).toBe("2026-10-06");
    expect(isDraftEmpty(empty)).toBe(true);
  });

  it("is not empty once anything useful is typed", () => {
    expect(isDraftEmpty(draft({ summary: "" }))).toBe(true);
    expect(isDraftEmpty(draft({ summary: "", workersOnSite: "0" }))).toBe(true);
    expect(isDraftEmpty(draft({ summary: "", workersOnSite: "6" }))).toBe(false);
    expect(isDraftEmpty(draft({ summary: "  ", delays: "Rain" }))).toBe(false);
    expect(isDraftEmpty(draft())).toBe(false);
  });

  it("is filled from an existing report", () => {
    const report: DailySiteReport = {
      id: "r1",
      projectId: "p1",
      reportDate: "2026-10-06",
      weatherText: null,
      workersOnSite: 12,
      summary: "Formwork done",
      workCompleted: "Level 2",
      delays: null,
      issues: null,
      nextSteps: "Pour slab",
      createdBy: "u1",
      createdAt: "2026-10-06T08:00:00Z",
      updatedAt: "2026-10-06T08:00:00Z",
    };

    expect(draftFromReport(report)).toEqual({
      reportDate: "2026-10-06",
      weatherText: "",
      workersOnSite: "12",
      summary: "Formwork done",
      workCompleted: "Level 2",
      delays: "",
      issues: "",
      nextSteps: "Pour slab",
    });
  });

  it("reads the number of workers", () => {
    expect(parseWorkers("")).toBe(0);
    expect(parseWorkers("  ")).toBe(0);
    expect(parseWorkers("12")).toBe(12);
    expect(parseWorkers(" 7 ")).toBe(7);
    expect(parseWorkers("-1")).toBeNull();
    expect(parseWorkers("1.5")).toBeNull();
    expect(parseWorkers("abc")).toBeNull();
    expect(parseWorkers("1234567890")).toBeNull();
  });
});

describe("validateDraft", () => {
  it("accepts a report with just a summary", () => {
    expect(validateDraft(draft())).toEqual({});
  });

  it("asks for a summary", () => {
    expect(validateDraft(draft({ summary: "   " })).summary).toBe(
      "Write a short summary of the day.",
    );
  });

  it("checks the date", () => {
    expect(validateDraft(draft({ reportDate: "" })).reportDate).toBe("Choose a valid date.");
  });

  it("checks the number of workers", () => {
    expect(validateDraft(draft({ workersOnSite: "abc" })).workersOnSite).toBe(
      "Enter a whole number of workers, 0 or more.",
    );
    expect(validateDraft(draft({ workersOnSite: "100001" })).workersOnSite).toBe(
      "That is more workers than can be recorded.",
    );
    expect(validateDraft(draft({ workersOnSite: "100000" })).workersOnSite).toBeUndefined();
  });

  it("limits the length of the texts", () => {
    const long = "x".repeat(20001);

    expect(validateDraft(draft({ summary: long })).summary).toContain("too long");
    expect(validateDraft(draft({ delays: long })).delays).toContain("too long");
    expect(validateDraft(draft({ weatherText: "w".repeat(501) })).weatherText).toContain("500");
    expect(validateDraft(draft({ issues: "x".repeat(20000) })).issues).toBeUndefined();
  });

  it("knows which step a field belongs to", () => {
    expect(stepForField("reportDate")).toBe("basics");
    expect(stepForField("workersOnSite")).toBe("basics");
    expect(stepForField("summary")).toBe("work");
    expect(stepForField("nextSteps")).toBe("work");
  });

  it("groups the problems by step", () => {
    const errors = validateDraft(draft({ summary: "", workersOnSite: "x" }));

    expect(Object.keys(errorsForStep("basics", errors))).toEqual(["workersOnSite"]);
    expect(Object.keys(errorsForStep("work", errors))).toEqual(["summary"]);
    expect(errorsForStep("photos", errors)).toEqual({});
    expect(firstStepWithError(errors)).toBe("basics");
    expect(firstStepWithError(validateDraft(draft({ summary: "" })))).toBe("work");
    expect(firstStepWithError({})).toBeNull();
  });
});

describe("what is sent", () => {
  it("builds a new report, with empty boxes as nothing", () => {
    expect(
      buildCreateRequest(
        draft({
          weatherText: "  Sunny ",
          workersOnSite: "14",
          summary: "  Slab poured  ",
          delays: "  ",
        }),
      ),
    ).toEqual({
      reportDate: "2026-10-06",
      weatherText: "Sunny",
      workersOnSite: 14,
      summary: "Slab poured",
      workCompleted: null,
      delays: null,
      issues: null,
      nextSteps: null,
    });
  });

  it("counts a blank number of workers as zero", () => {
    expect(buildCreateRequest(draft()).workersOnSite).toBe(0);
    expect(buildUpdateRequest(draft()).workersOnSite).toBe(0);
  });

  it("builds an update that clears a box with an empty string", () => {
    expect(
      buildUpdateRequest(
        draft({ weatherText: "Rain", workersOnSite: "3", delays: "", issues: " Late delivery " }),
      ),
    ).toEqual({
      weatherText: "Rain",
      workersOnSite: 3,
      summary: "Slab poured on level 2",
      workCompleted: "",
      delays: "",
      issues: "Late delivery",
      nextSteps: "",
    });
  });
});

describe("saving the draft on the phone", () => {
  const key = draftKey("org-1", "proj-1", "2026-10-06");

  it("builds a key per organization, project and day", () => {
    expect(key).toBe("buildflow.siteReportDraft.org-1.proj-1.2026-10-06");
  });

  it("writes and reads a draft back", () => {
    const storage = memoryStorage();
    const saved = draft({ weatherText: "Cloudy", workersOnSite: "5" });

    expect(writeDraft(storage, key, saved, new Date("2026-10-06T12:32:00Z"))).toBe(true);

    expect(readDraft(storage, key)).toEqual({
      savedAt: "2026-10-06T12:32:00.000Z",
      draft: saved,
    });
  });

  it("removes a draft", () => {
    const storage = memoryStorage();

    writeDraft(storage, key, draft(), new Date());
    removeDraft(storage, key);

    expect(readDraft(storage, key)).toBeNull();
  });

  it("copes with no storage at all", () => {
    expect(readDraft(null, key)).toBeNull();
    expect(writeDraft(null, key, draft(), new Date())).toBe(false);
    removeDraft(null, key);
  });

  it("reports false when the phone will not keep it", () => {
    const full: StorageLike = {
      getItem: () => null,
      setItem: () => {
        throw new Error("QuotaExceededError");
      },
      removeItem: () => {
        throw new Error("blocked");
      },
    };

    expect(writeDraft(full, key, draft(), new Date())).toBe(false);
    removeDraft(full, key);
  });

  it("ignores a draft that cannot be read", () => {
    const storage = memoryStorage();

    storage.data[key] = "{not json";
    expect(readDraft(storage, key)).toBeNull();

    storage.data[key] = JSON.stringify("text");
    expect(readDraft(storage, key)).toBeNull();

    storage.data[key] = JSON.stringify({ savedAt: "x", draft: null });
    expect(readDraft(storage, key)).toBeNull();

    storage.data[key] = JSON.stringify({ savedAt: "x", draft: { reportDate: "nope" } });
    expect(readDraft(storage, key)).toBeNull();
  });

  it("turns missing or odd fields into empty text", () => {
    const storage = memoryStorage();

    storage.data[key] = JSON.stringify({
      savedAt: 5,
      draft: { reportDate: "2026-10-06", summary: "Hello", workersOnSite: 4, delays: null },
    });

    expect(readDraft(storage, key)).toEqual({
      savedAt: "",
      draft: { ...emptyDraft("2026-10-06"), summary: "Hello" },
    });
  });
});

describe("photos", () => {
  it("names the categories the way the site page does", () => {
    expect(PHOTO_CATEGORIES.map((item) => item.value)).toEqual([
      "BEFORE",
      "PROGRESS",
      "AFTER",
      "DELIVERY",
      "DAMAGE",
      "SAFETY_ISSUE",
      "OTHER",
    ]);
    expect(photoCategoryLabel("AFTER")).toBe("Completed");
    expect(photoCategoryLabel("SAFETY_ISSUE")).toBe("Safety issue");
  });

  it("writes sizes for people", () => {
    expect(formatSize(100)).toBe("1 KB");
    expect(formatSize(250_000)).toBe("244 KB");
    expect(formatSize(1_572_864)).toBe("1.5 MB");
  });

  it("builds the request the server expects", () => {
    expect(
      buildPhotoRequest(
        photo("a", { caption: "  Level 2 slab ", category: "AFTER", clientVisible: true }),
      ),
    ).toEqual({
      fileName: "a.jpg",
      mimeType: "image/jpeg",
      base64Data: "data:image/jpeg;base64,a",
      caption: "Level 2 slab",
      category: "AFTER",
      clientVisible: true,
    });

    expect(buildPhotoRequest(photo("b", { caption: "  " })).caption).toBeNull();
  });

  it("picks the photos that still have to be sent", () => {
    const photos = [
      photo("a", { status: "done" }),
      photo("b", { status: "failed" }),
      photo("c"),
      photo("d", { status: "uploading" }),
    ];

    expect(pendingPhotos(photos).map((item) => item.id)).toEqual(["b", "c"]);
  });

  it("summarises where the photos stand", () => {
    expect(photoSummary([])).toBe("No photos added");
    expect(photoSummary([photo("a")])).toBe("1 photo ready to send");
    expect(photoSummary([photo("a"), photo("b")])).toBe("2 photos ready to send");
    expect(photoSummary([photo("a", { status: "done" })])).toBe("The photo was sent");
    expect(photoSummary([photo("a", { status: "done" }), photo("b", { status: "done" })])).toBe(
      "All 2 photos were sent",
    );
    expect(photoSummary([photo("a", { status: "done" }), photo("b", { status: "failed" })])).toBe(
      "1 of 2 photos sent, 1 not sent yet",
    );
  });
});

describe("errorMessage", () => {
  it("explains a lost connection", () => {
    expect(errorMessage(new TypeError("Failed to fetch"), "x")).toBe(
      "There is no connection right now. Check your signal and try again.",
    );
  });

  it("uses the message of an error", () => {
    expect(errorMessage(new Error("Only JPEG, PNG and WebP site photos are supported."), "x")).toBe(
      "Only JPEG, PNG and WebP site photos are supported.",
    );
    expect(errorMessage({ message: "Nope" }, "x")).toBe("Nope");
  });

  it("falls back to the given text", () => {
    expect(errorMessage(undefined, "Fallback")).toBe("Fallback");
    expect(errorMessage(new Error("  "), "Fallback")).toBe("Fallback");
    expect(errorMessage({ message: 5 }, "Fallback")).toBe("Fallback");
  });
});

describe("uploadPending", () => {
  it("sends the waiting photos one by one and reports each", async () => {
    const photos = [photo("a"), photo("b", { status: "done" }), photo("c")];
    const order: string[] = [];
    const events: string[] = [];

    const result = await uploadPending(
      photos,
      async (item) => {
        order.push(item.id);
      },
      (id: string, status: PhotoStatus, error: string) => {
        events.push(`${id}:${status}:${error}`);
      },
    );

    expect(result).toEqual({ sent: 2, failed: 0 });
    expect(order).toEqual(["a", "c"]);
    expect(events).toEqual(["a:uploading:", "a:done:", "c:uploading:", "c:done:"]);
  });

  it("carries on after a photo fails and keeps its message", async () => {
    const photos = [photo("a"), photo("b"), photo("c")];
    const events: string[] = [];

    const result = await uploadPending(
      photos,
      async (item) => {
        if (item.id === "b") {
          throw new Error("A site photo must be between 1 byte and 5 MB.");
        }
      },
      (id: string, status: PhotoStatus, error: string) => {
        events.push(`${id}:${status}:${error}`);
      },
    );

    expect(result).toEqual({ sent: 2, failed: 1 });
    expect(events).toContain("b:failed:A site photo must be between 1 byte and 5 MB.");
    expect(events).toContain("c:done:");
  });

  it("does nothing when everything is already sent", async () => {
    let called = 0;

    const result = await uploadPending(
      [photo("a", { status: "done" })],
      async () => {
        called += 1;
      },
      () => {},
    );

    expect(result).toEqual({ sent: 0, failed: 0 });
    expect(called).toBe(0);
  });

  it("tries failed photos again", async () => {
    const photos = [photo("a", { status: "failed", error: "No connection" })];
    const events: string[] = [];

    const result = await uploadPending(
      photos,
      async () => {},
      (id: string, status: PhotoStatus) => {
        events.push(`${id}:${status}`);
      },
    );

    expect(result).toEqual({ sent: 1, failed: 0 });
    expect(events).toEqual(["a:uploading", "a:done"]);
  });
});
