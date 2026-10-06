// Getting a phone photo ready for the server.
//
// A modern phone takes pictures of 4 to 12 MB, and the server accepts at most
// 5 MB, so most photos taken on site would be refused. This shrinks them in
// the browser first: smaller uploads on a weak signal, and nothing refused.

export const PHOTO_ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"];

// Pictures longer than this on their longest side are scaled down. 1920 pixels
// is sharp on a full screen and plenty for a site record.
export const PHOTO_MAX_EDGE = 1920;

// A file this small is sent as it is, untouched.
export const PHOTO_PASS_THROUGH_BYTES = 1_000_000;

// The size the result should reach when it can.
export const PHOTO_TARGET_BYTES = 1_500_000;

// Never send more than this: the server limit is 5 MB.
export const PHOTO_LIMIT_BYTES = 4_500_000;

export const PHOTO_QUALITIES = [0.85, 0.72, 0.6];

// When even the lowest quality is too big, the picture is made smaller.
export const PHOTO_SHRINK_STEPS = [1, 0.75, 0.56, 0.42];

export class PhotoError extends Error {}

export interface DecodedImage {
  width: number;
  height: number;
}

export interface PhotoDeps<Image extends DecodedImage = DecodedImage> {
  decode(file: Blob): Promise<Image>;
  encode(image: Image, width: number, height: number, quality: number): Promise<Blob>;
  toDataUrl(blob: Blob): Promise<string>;
  release?(image: Image): void;
}

export interface PreparedPhoto {
  fileName: string;
  mimeType: string;
  dataUrl: string;
  sizeBytes: number;
  // The new size when the picture was re-encoded; null when sent untouched.
  width: number | null;
  height: number | null;
  resized: boolean;
}

// The size that fits inside the limit, keeping the shape. Never enlarges.
export function fitWithin(
  width: number,
  height: number,
  maxEdge: number,
): { width: number; height: number } {
  const longest = Math.max(width, height);

  if (longest <= maxEdge || longest <= 0) {
    return { width: Math.max(1, Math.round(width)), height: Math.max(1, Math.round(height)) };
  }

  const ratio = maxEdge / longest;

  return {
    width: Math.max(1, Math.round(width * ratio)),
    height: Math.max(1, Math.round(height * ratio)),
  };
}

export function extensionFor(mimeType: string): string {
  switch (mimeType) {
    case "image/png":
      return ".png";
    case "image/webp":
      return ".webp";
    default:
      return ".jpg";
  }
}

// A safe name that ends in an extension the server accepts and that matches
// the picture's real type.
export function photoFileName(original: string, mimeType: string): string {
  const extension = extensionFor(mimeType);
  const dot = original.lastIndexOf(".");
  const stem = (dot > 0 ? original.slice(0, dot) : original)
    .replace(/[^A-Za-z0-9._-]+/g, "-")
    .replace(/^[-.]+|[-.]+$/g, "")
    .slice(0, 80);

  return `${stem || "site-photo"}${extension}`;
}

// Makes the photo small enough to send. Throws a PhotoError with a message a
// person can act on.
export async function preparePhoto<Image extends DecodedImage>(
  file: File,
  deps: PhotoDeps<Image>,
): Promise<PreparedPhoto> {
  if (file.size === 0) {
    throw new PhotoError("That photo is empty. Please take it again.");
  }

  const type = file.type.toLowerCase();

  // Already small and in a format the server takes: nothing to do.
  if (PHOTO_ALLOWED_TYPES.includes(type) && file.size <= PHOTO_PASS_THROUGH_BYTES) {
    return {
      fileName: photoFileName(file.name, type),
      mimeType: type,
      dataUrl: await deps.toDataUrl(file),
      sizeBytes: file.size,
      width: null,
      height: null,
      resized: false,
    };
  }

  let image: Image;

  try {
    image = await deps.decode(file);
  } catch {
    throw new PhotoError(
      "This photo could not be opened. Use a JPEG, PNG or WebP picture, or take it again.",
    );
  }

  try {
    const base = fitWithin(image.width, image.height, PHOTO_MAX_EDGE);
    let smallest: { blob: Blob; width: number; height: number } | null = null;

    for (const scale of PHOTO_SHRINK_STEPS) {
      const width = Math.max(1, Math.round(base.width * scale));
      const height = Math.max(1, Math.round(base.height * scale));

      for (const quality of PHOTO_QUALITIES) {
        const blob = await deps.encode(image, width, height, quality);

        if (blob.size > 0 && blob.size <= PHOTO_TARGET_BYTES) {
          return await finish(file, blob, width, height, deps);
        }

        if (blob.size > 0 && (smallest === null || blob.size < smallest.blob.size)) {
          smallest = { blob, width, height };
        }
      }

      // Not at the target size, but small enough to send: good enough.
      if (smallest !== null && smallest.blob.size <= PHOTO_LIMIT_BYTES) {
        return await finish(file, smallest.blob, smallest.width, smallest.height, deps);
      }
    }
  } catch (caught) {
    if (caught instanceof PhotoError) {
      throw caught;
    }

    throw new PhotoError("This photo could not be prepared. Please take it again.");
  } finally {
    deps.release?.(image);
  }

  throw new PhotoError("This photo is too large to send, even after shrinking it.");
}

async function finish<Image extends DecodedImage>(
  file: File,
  blob: Blob,
  width: number,
  height: number,
  deps: PhotoDeps<Image>,
): Promise<PreparedPhoto> {
  return {
    fileName: photoFileName(file.name, "image/jpeg"),
    mimeType: "image/jpeg",
    dataUrl: await deps.toDataUrl(blob),
    sizeBytes: blob.size,
    width,
    height,
    resized: true,
  };
}

// ---------------------------------------------------------------------------
// The real thing, in the browser
// ---------------------------------------------------------------------------

interface BrowserImage extends DecodedImage {
  source: CanvasImageSource;
  close(): void;
}

function readAsDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result === "string") {
        resolve(reader.result);
      } else {
        reject(new Error("Unable to read the photo."));
      }
    };

    reader.onerror = () => {
      reject(reader.error ?? new Error("Unable to read the photo."));
    };

    reader.readAsDataURL(blob);
  });
}

async function decodeInBrowser(file: Blob): Promise<BrowserImage> {
  // Turns the picture the right way up, as the camera recorded it.
  if (typeof createImageBitmap === "function") {
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: "from-image" });

      return {
        width: bitmap.width,
        height: bitmap.height,
        source: bitmap,
        close: () => bitmap.close(),
      };
    } catch {
      // Fall back to an image element below.
    }
  }

  const url = URL.createObjectURL(file);

  try {
    const element = new Image();

    element.decoding = "async";
    element.src = url;
    await element.decode();

    return {
      width: element.naturalWidth,
      height: element.naturalHeight,
      source: element,
      close: () => {},
    };
  } finally {
    URL.revokeObjectURL(url);
  }
}

function encodeInBrowser(
  image: BrowserImage,
  width: number,
  height: number,
  quality: number,
): Promise<Blob> {
  const canvas = document.createElement("canvas");

  canvas.width = width;
  canvas.height = height;

  const context = canvas.getContext("2d");

  if (!context) {
    return Promise.reject(new Error("The browser cannot draw pictures."));
  }

  // JPEG has no transparency: put the picture on white.
  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, width, height);
  context.drawImage(image.source, 0, 0, width, height);

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob);
        } else {
          reject(new Error("The picture could not be saved."));
        }
      },
      "image/jpeg",
      quality,
    );
  });
}

export const browserPhotoDeps: PhotoDeps<BrowserImage> = {
  decode: decodeInBrowser,
  encode: encodeInBrowser,
  toDataUrl: readAsDataUrl,
  release: (image) => image.close(),
};

export function prepareBrowserPhoto(file: File): Promise<PreparedPhoto> {
  return preparePhoto(file, browserPhotoDeps);
}
