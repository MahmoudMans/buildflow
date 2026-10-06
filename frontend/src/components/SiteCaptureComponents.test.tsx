import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import type { CaptureDraft, CapturePhoto } from "@/lib/site-capture";

import { BasicsStep, WorkStep } from "./SiteCaptureFields";
import { SiteCapturePhotos } from "./SiteCapturePhotos";
import { SiteCaptureReview } from "./SiteCaptureReview";
import { SiteCaptureStepper } from "./SiteCaptureStepper";

afterEach(() => {
  cleanup();
});

function draft(change: Partial<CaptureDraft> = {}): CaptureDraft {
  return {
    reportDate: "2026-10-06",
    weatherText: "",
    workersOnSite: "",
    summary: "",
    workCompleted: "",
    delays: "",
    issues: "",
    nextSteps: "",
    ...change,
  };
}

function photo(id: string, change: Partial<CapturePhoto> = {}): CapturePhoto {
  return {
    id,
    fileName: `${id}.jpg`,
    mimeType: "image/jpeg",
    dataUrl: "data:image/jpeg;base64,AAAA",
    sizeBytes: 250_000,
    category: "PROGRESS",
    caption: "",
    clientVisible: false,
    status: "ready",
    error: "",
    ...change,
  };
}

describe("SiteCaptureStepper", () => {
  it("marks the current step and lets you go back to finished ones", () => {
    const onGoTo = vi.fn();

    render(<SiteCaptureStepper current="photos" onGoTo={onGoTo} />);

    const current = screen.getByText("Photos").closest("li");

    expect(current?.getAttribute("aria-current")).toBe("step");
    expect(screen.queryByRole("button", { name: "Back to Send" })).toBeNull();
    expect(screen.queryByRole("button", { name: "Back to Photos" })).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: "Back to Work" }));

    expect(onGoTo).toHaveBeenCalledWith("work");
    expect(screen.getByRole("button", { name: "Back to Basics" })).toBeTruthy();
  });

  it("has no way back on the first step", () => {
    render(<SiteCaptureStepper current="basics" onGoTo={vi.fn()} />);

    expect(screen.queryAllByRole("button")).toHaveLength(0);
  });
});

describe("BasicsStep", () => {
  it("fills the weather from a quick choice and clears it when tapped again", () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <BasicsStep draft={draft()} errors={{}} today="2026-10-06" onChange={onChange} />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Rain" }));
    expect(onChange).toHaveBeenCalledWith({ weatherText: "Rain" });

    rerender(
      <BasicsStep
        draft={draft({ weatherText: "Rain" })}
        errors={{}}
        today="2026-10-06"
        onChange={onChange}
      />,
    );

    expect(screen.getByRole("button", { name: "Rain" }).getAttribute("aria-pressed")).toBe("true");

    fireEvent.click(screen.getByRole("button", { name: "Rain" }));
    expect(onChange).toHaveBeenLastCalledWith({ weatherText: "" });
  });

  it("counts workers up and down and never below zero", () => {
    const onChange = vi.fn();

    const { rerender } = render(
      <BasicsStep
        draft={draft({ workersOnSite: "2" })}
        errors={{}}
        today="2026-10-06"
        onChange={onChange}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "One more worker" }));
    expect(onChange).toHaveBeenLastCalledWith({ workersOnSite: "3" });

    fireEvent.click(screen.getByRole("button", { name: "One fewer worker" }));
    expect(onChange).toHaveBeenLastCalledWith({ workersOnSite: "1" });

    rerender(
      <BasicsStep
        draft={draft({ workersOnSite: "0" })}
        errors={{}}
        today="2026-10-06"
        onChange={onChange}
      />,
    );

    expect(
      (screen.getByRole("button", { name: "One fewer worker" }) as HTMLButtonElement).disabled,
    ).toBe(true);
  });

  it("passes on what is typed and limits the date to today", () => {
    const onChange = vi.fn();

    render(<BasicsStep draft={draft()} errors={{}} today="2026-10-06" onChange={onChange} />);

    const date = screen.getByLabelText("Report date") as HTMLInputElement;

    expect(date.max).toBe("2026-10-06");
    expect(date.value).toBe("2026-10-06");

    fireEvent.change(screen.getByLabelText("Workers on site"), { target: { value: "12" } });
    expect(onChange).toHaveBeenLastCalledWith({ workersOnSite: "12" });

    fireEvent.change(screen.getByLabelText("Weather"), { target: { value: "Fog" } });
    expect(onChange).toHaveBeenLastCalledWith({ weatherText: "Fog" });
  });

  it("shows problems next to the field", () => {
    render(
      <BasicsStep
        draft={draft({ workersOnSite: "x" })}
        errors={{ workersOnSite: "Enter a whole number of workers, 0 or more." }}
        today="2026-10-06"
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Enter a whole number of workers, 0 or more.")).toBeTruthy();
    expect(screen.getByLabelText("Workers on site").getAttribute("aria-invalid")).toBe("true");
  });
});

describe("WorkStep", () => {
  it("asks for a summary and passes on what is typed", () => {
    const onChange = vi.fn();

    render(<WorkStep draft={draft()} errors={{}} onChange={onChange} />);

    expect(screen.getByText("(required)")).toBeTruthy();

    fireEvent.change(screen.getByLabelText(/^Summary/), { target: { value: "Slab poured" } });
    expect(onChange).toHaveBeenLastCalledWith({ summary: "Slab poured" });

    fireEvent.change(screen.getByLabelText("Delays"), { target: { value: "Rain" } });
    expect(onChange).toHaveBeenLastCalledWith({ delays: "Rain" });

    fireEvent.change(screen.getByLabelText("Next steps"), { target: { value: "Pour level 3" } });
    expect(onChange).toHaveBeenLastCalledWith({ nextSteps: "Pour level 3" });
  });

  it("shows the problem under the summary", () => {
    render(
      <WorkStep
        draft={draft()}
        errors={{ summary: "Write a short summary of the day." }}
        onChange={vi.fn()}
      />,
    );

    expect(screen.getByText("Write a short summary of the day.")).toBeTruthy();
    expect(screen.getByLabelText(/^Summary/).getAttribute("aria-invalid")).toBe("true");
  });
});

describe("SiteCapturePhotos", () => {
  function setup(change: Partial<Parameters<typeof SiteCapturePhotos>[0]> = {}) {
    const handlers = {
      onAddFiles: vi.fn(),
      onRemove: vi.fn(),
      onChange: vi.fn(),
    };

    render(
      <SiteCapturePhotos
        photos={[]}
        preparing={0}
        notice=""
        maxPhotos={20}
        locked={false}
        {...handlers}
        {...change}
      />,
    );

    return handlers;
  }

  it("says so when there are no photos", () => {
    setup();

    expect(screen.getByText("No photos yet. You can send the report without them.")).toBeTruthy();
  });

  it("hands the chosen pictures over, from the camera and from the gallery", () => {
    const handlers = setup();
    const first = new File(["a"], "wall.jpg", { type: "image/jpeg" });
    const second = new File(["b"], "slab.jpg", { type: "image/jpeg" });

    fireEvent.change(screen.getByLabelText("Take a photo"), { target: { files: [first] } });
    expect(handlers.onAddFiles).toHaveBeenLastCalledWith([first]);

    fireEvent.change(screen.getByLabelText("Choose from gallery"), {
      target: { files: [first, second] },
    });
    expect(handlers.onAddFiles).toHaveBeenLastCalledWith([first, second]);
  });

  it("opens the rear camera and takes several from the gallery", () => {
    setup();

    expect(screen.getByLabelText("Take a photo").getAttribute("capture")).toBe("environment");
    expect((screen.getByLabelText("Choose from gallery") as HTMLInputElement).multiple).toBe(true);
  });

  it("stops adding when the report is full or while sending", () => {
    setup({ photos: [photo("a")], maxPhotos: 1 });

    expect((screen.getByLabelText("Take a photo") as HTMLInputElement).disabled).toBe(true);
    expect((screen.getByLabelText("Choose from gallery") as HTMLInputElement).disabled).toBe(true);
  });

  it("lets you describe, categorise and remove a photo", () => {
    const handlers = setup({ photos: [photo("a", { fileName: "wall.jpg" })] });

    expect(screen.getByText("wall.jpg")).toBeTruthy();
    expect(screen.getByText("244 KB")).toBeTruthy();
    expect(screen.getByText("Ready")).toBeTruthy();
    expect(screen.getByRole("img", { name: "Photo 1" })).toBeTruthy();

    fireEvent.change(screen.getByLabelText("Category"), { target: { value: "DAMAGE" } });
    expect(handlers.onChange).toHaveBeenLastCalledWith("a", { category: "DAMAGE" });

    fireEvent.change(screen.getByLabelText("Caption"), { target: { value: "Cracked beam" } });
    expect(handlers.onChange).toHaveBeenLastCalledWith("a", { caption: "Cracked beam" });

    fireEvent.click(screen.getByLabelText("Show this photo to the client"));
    expect(handlers.onChange).toHaveBeenLastCalledWith("a", { clientVisible: true });

    fireEvent.click(screen.getByRole("button", { name: "Remove photo 1" }));
    expect(handlers.onRemove).toHaveBeenCalledWith("a");
  });

  it("freezes a photo that was sent and explains one that failed", () => {
    setup({
      photos: [
        photo("a", { status: "done" }),
        photo("b", { status: "failed", error: "There is no connection right now." }),
      ],
    });

    expect(screen.getByText("Sent")).toBeTruthy();
    expect(screen.getByText("Not sent")).toBeTruthy();
    expect(screen.getByText("There is no connection right now.")).toBeTruthy();

    const categories = screen.getAllByLabelText("Category") as HTMLSelectElement[];

    expect(categories[0].disabled).toBe(true);
    expect(categories[1].disabled).toBe(false);
    expect(screen.queryByRole("button", { name: "Remove photo 1" })).toBeNull();
    expect(screen.getByRole("button", { name: "Remove photo 2" })).toBeTruthy();
  });

  it("shows pictures being prepared and any that could not be added", () => {
    setup({ preparing: 2, notice: "x.heic: This photo could not be opened." });

    expect(screen.getByText("Getting 2 photos ready...")).toBeTruthy();
    expect(screen.getByRole("alert").textContent).toContain("could not be opened");
  });
});

describe("SiteCaptureReview", () => {
  it("shows what will be sent and leaves out empty optional rows", () => {
    render(
      <SiteCaptureReview
        draft={draft({
          weatherText: "Sunny",
          workersOnSite: "14",
          summary: "Slab poured",
          delays: "Late truck",
        })}
        photos={[photo("a")]}
        errors={{}}
        onEdit={vi.fn()}
      />,
    );

    expect(screen.getByText("Tue 6 Oct 2026")).toBeTruthy();
    expect(screen.getByText("Sunny")).toBeTruthy();
    expect(screen.getByText("14")).toBeTruthy();
    expect(screen.getByText("Slab poured")).toBeTruthy();
    expect(screen.getByText("Late truck")).toBeTruthy();
    expect(screen.queryByText("Issues or problems")).toBeNull();
    expect(screen.getByText("1 photo ready to send")).toBeTruthy();
    expect(screen.getByRole("img", { name: "Photo 1" })).toBeTruthy();
  });

  it("says when the weather was not recorded", () => {
    render(
      <SiteCaptureReview
        draft={draft({ summary: "Slab poured" })}
        photos={[]}
        errors={{}}
        onEdit={vi.fn()}
      />,
    );

    expect(screen.getByText("Not recorded")).toBeTruthy();
    expect(screen.getByText("No photos added")).toBeTruthy();
  });

  it("takes you to the step where something can be changed", () => {
    const onEdit = vi.fn();

    render(
      <SiteCaptureReview
        draft={draft({ summary: "Slab poured" })}
        photos={[]}
        errors={{}}
        onEdit={onEdit}
      />,
    );

    fireEvent.click(screen.getByRole("button", { name: "Change summary" }));
    expect(onEdit).toHaveBeenLastCalledWith("work");

    fireEvent.click(screen.getByRole("button", { name: "Change date" }));
    expect(onEdit).toHaveBeenLastCalledWith("basics");

    fireEvent.click(screen.getByRole("button", { name: "Change workers on site" }));
    expect(onEdit).toHaveBeenLastCalledWith("basics");

    fireEvent.click(screen.getByRole("button", { name: "Change photos" }));
    expect(onEdit).toHaveBeenLastCalledWith("photos");
  });

  it("lists problems and links each one to its step", () => {
    const onEdit = vi.fn();

    render(
      <SiteCaptureReview
        draft={draft()}
        photos={[]}
        errors={{ summary: "Write a short summary of the day." }}
        onEdit={onEdit}
      />,
    );

    expect(screen.getByRole("alert").textContent).toContain("Fix this before sending");

    fireEvent.click(screen.getByRole("button", { name: "Write a short summary of the day." }));
    expect(onEdit).toHaveBeenCalledWith("work");
  });
});
