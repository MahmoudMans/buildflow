import { describe, expect, it } from "vitest";

import {
  PHOTO_LIMIT_BYTES,
  PHOTO_MAX_EDGE,
  PHOTO_PASS_THROUGH_BYTES,
  PHOTO_TARGET_BYTES,
  PhotoError,
  extensionFor,
  fitWithin,
  photoFileName,
  preparePhoto,
} from "./site-photo";
import type { DecodedImage, PhotoDeps } from "./site-photo";

function file(name: string, type: string, size: number): File {
  return { name, type, size } as unknown as File;
}

interface Call {
  width: number;
  height: number;
  quality: number;
}

// A fake camera pipeline: the encoded size follows a function of the call.
function deps(
  image: DecodedImage,
  sizeFor: (call: Call, index: number) => number,
  options: { failDecode?: boolean; failEncode?: boolean } = {},
): PhotoDeps & { calls: Call[]; released: number; decoded: number } {
  const state = {
    calls: [] as Call[],
    released: 0,
    decoded: 0,
    async decode() {
      state.decoded += 1;

      if (options.failDecode) {
        throw new Error("cannot decode");
      }

      return image;
    },
    async encode(_image: DecodedImage, width: number, height: number, quality: number) {
      if (options.failEncode) {
        throw new Error("cannot encode");
      }

      const call = { width, height, quality };

      state.calls.push(call);

      return { size: sizeFor(call, state.calls.length - 1) } as unknown as Blob;
    },
    async toDataUrl(blob: Blob) {
      return `data:image/jpeg;base64,size${blob.size}`;
    },
    release() {
      state.released += 1;
    },
  };

  return state;
}

describe("fitWithin", () => {
  it("scales the longest side down to the limit", () => {
    expect(fitWithin(4000, 3000, 1920)).toEqual({ width: 1920, height: 1440 });
    expect(fitWithin(3000, 4000, 1920)).toEqual({ width: 1440, height: 1920 });
  });

  it("never makes a picture bigger", () => {
    expect(fitWithin(800, 600, 1920)).toEqual({ width: 800, height: 600 });
    expect(fitWithin(1920, 1080, 1920)).toEqual({ width: 1920, height: 1080 });
  });

  it("keeps at least one pixel", () => {
    expect(fitWithin(10000, 1, 1920)).toEqual({ width: 1920, height: 1 });
  });
});

describe("file names", () => {
  it("gives each type its extension", () => {
    expect(extensionFor("image/jpeg")).toBe(".jpg");
    expect(extensionFor("image/png")).toBe(".png");
    expect(extensionFor("image/webp")).toBe(".webp");
    expect(extensionFor("image/heic")).toBe(".jpg");
  });

  it("keeps a clean name and fixes the extension to the real type", () => {
    expect(photoFileName("IMG_2041.JPG", "image/jpeg")).toBe("IMG_2041.jpg");
    expect(photoFileName("wall.heic", "image/jpeg")).toBe("wall.jpg");
    expect(photoFileName("Level 2 / north wall (1).png", "image/png")).toBe(
      "Level-2-north-wall-1.png",
    );
  });

  it("invents a name when there is nothing usable", () => {
    expect(photoFileName("", "image/jpeg")).toBe("site-photo.jpg");
    expect(photoFileName("...", "image/webp")).toBe("site-photo.webp");
    expect(photoFileName("image", "image/jpeg")).toBe("image.jpg");
  });

  it("shortens very long names", () => {
    const name = photoFileName(`${"a".repeat(300)}.jpg`, "image/jpeg");

    expect(name.length).toBeLessThanOrEqual(84);
    expect(name.endsWith(".jpg")).toBe(true);
  });
});

describe("preparePhoto", () => {
  it("sends a small photo as it is, without opening it", async () => {
    const pipeline = deps({ width: 100, height: 100 }, () => 1);

    const result = await preparePhoto(
      file("image.jpg", "image/jpeg", PHOTO_PASS_THROUGH_BYTES),
      pipeline,
    );

    expect(result.resized).toBe(false);
    expect(result.mimeType).toBe("image/jpeg");
    expect(result.sizeBytes).toBe(PHOTO_PASS_THROUGH_BYTES);
    expect(result.width).toBeNull();
    expect(pipeline.decoded).toBe(0);
  });

  it("shrinks a big phone photo to the longest side limit", async () => {
    const pipeline = deps({ width: 4032, height: 3024 }, () => 900_000);

    const result = await preparePhoto(file("IMG_1.jpg", "image/jpeg", 6_000_000), pipeline);

    expect(result.resized).toBe(true);
    expect(result.mimeType).toBe("image/jpeg");
    expect(result.fileName).toBe("IMG_1.jpg");
    expect(result.width).toBe(PHOTO_MAX_EDGE);
    expect(result.height).toBe(1440);
    expect(result.sizeBytes).toBe(900_000);
    expect(result.dataUrl).toBe("data:image/jpeg;base64,size900000");
    expect(pipeline.calls).toHaveLength(1);
    expect(pipeline.calls[0].quality).toBe(0.85);
    expect(pipeline.released).toBe(1);
  });

  it("lowers the quality until the picture reaches the target", async () => {
    const pipeline = deps({ width: 4000, height: 3000 }, (call) =>
      call.quality >= 0.85 ? 2_400_000 : call.quality >= 0.72 ? 1_900_000 : 1_200_000,
    );

    const result = await preparePhoto(file("a.jpg", "image/jpeg", 7_000_000), pipeline);

    expect(pipeline.calls.map((call) => call.quality)).toEqual([0.85, 0.72, 0.6]);
    expect(result.sizeBytes).toBe(1_200_000);
    expect(result.sizeBytes).toBeLessThanOrEqual(PHOTO_TARGET_BYTES);
  });

  it("accepts the smallest result when it is under the limit but over the target", async () => {
    const pipeline = deps({ width: 4000, height: 3000 }, (call) =>
      call.quality >= 0.85 ? 4_000_000 : call.quality >= 0.72 ? 3_000_000 : 2_000_000,
    );

    const result = await preparePhoto(file("a.png", "image/png", 9_000_000), pipeline);

    expect(result.sizeBytes).toBe(2_000_000);
    expect(result.mimeType).toBe("image/jpeg");
    expect(result.fileName).toBe("a.jpg");
    expect(pipeline.calls).toHaveLength(3);
  });

  it("makes the picture smaller when quality alone is not enough", async () => {
    const pipeline = deps({ width: 4000, height: 3000 }, (call) =>
      call.width >= 1920 ? 6_000_000 : 1_000_000,
    );

    const result = await preparePhoto(file("big.jpg", "image/jpeg", 12_000_000), pipeline);

    expect(result.sizeBytes).toBe(1_000_000);
    expect(result.width).toBe(1440);
    expect(result.height).toBe(1080);
    expect(pipeline.calls).toHaveLength(4);
  });

  it("gives up with a clear message when it can never fit", async () => {
    const pipeline = deps({ width: 4000, height: 3000 }, () => PHOTO_LIMIT_BYTES + 1);

    let error: unknown = null;

    try {
      await preparePhoto(file("huge.jpg", "image/jpeg", 40_000_000), pipeline);
    } catch (caught) {
      error = caught;
    }

    expect(error instanceof PhotoError).toBe(true);
    expect((error as Error).message).toContain("too large");
    expect(pipeline.released).toBe(1);
  });

  it("tries to open a type it does not know, such as HEIC", async () => {
    const pipeline = deps({ width: 3000, height: 2000 }, () => 700_000);

    const result = await preparePhoto(file("IMG_9.HEIC", "image/heic", 3_000_000), pipeline);

    expect(result.resized).toBe(true);
    expect(result.fileName).toBe("IMG_9.jpg");
    expect(result.mimeType).toBe("image/jpeg");
  });

  it("explains when the photo cannot be opened", async () => {
    const pipeline = deps({ width: 1, height: 1 }, () => 1, { failDecode: true });

    let error: unknown = null;

    try {
      await preparePhoto(file("x.heic", "image/heic", 3_000_000), pipeline);
    } catch (caught) {
      error = caught;
    }

    expect(error instanceof PhotoError).toBe(true);
    expect((error as Error).message).toContain("could not be opened");
  });

  it("explains when the picture cannot be saved again", async () => {
    const pipeline = deps({ width: 4000, height: 3000 }, () => 1, { failEncode: true });

    let error: unknown = null;

    try {
      await preparePhoto(file("x.jpg", "image/jpeg", 6_000_000), pipeline);
    } catch (caught) {
      error = caught;
    }

    expect(error instanceof PhotoError).toBe(true);
    expect((error as Error).message).toContain("could not be prepared");
    expect(pipeline.released).toBe(1);
  });

  it("refuses an empty file", async () => {
    const pipeline = deps({ width: 1, height: 1 }, () => 1);

    let error: unknown = null;

    try {
      await preparePhoto(file("x.jpg", "image/jpeg", 0), pipeline);
    } catch (caught) {
      error = caught;
    }

    expect(error instanceof PhotoError).toBe(true);
    expect((error as Error).message).toContain("empty");
  });
});
