function apiBaseUrl() {
  const configured =
    process.env.NEXT_PUBLIC_API_BASE_URL
    ?? process.env.NEXT_PUBLIC_API_URL
    ?? "http://localhost:8080/api/v1";

  const normalized =
    configured.replace(/\/+$/, "");

  return normalized.endsWith("/api/v1")
    ? normalized
    : `${normalized}/api/v1`;
}

function filenameFromDisposition(
  disposition: string | null,
  fallback: string
) {
  if (!disposition) {
    return fallback;
  }

  const utfMatch =
    disposition.match(
      /filename\*=UTF-8''([^;]+)/i
    );

  if (utfMatch?.[1]) {
    try {
      return decodeURIComponent(
        utfMatch[1]
      );
    } catch {
      return fallback;
    }
  }

  const plainMatch =
    disposition.match(
      /filename="?([^";]+)"?/i
    );

  return plainMatch?.[1]
    ?? fallback;
}

export async function downloadApiFile(
  path: string,
  organizationId: string,
  fallbackFilename: string
) {
  const response =
    await fetch(
      `${apiBaseUrl()}${path}`,
      {
        method: "GET",
        credentials: "include",
        headers: {
          "X-Organization-Id":
            organizationId,
        },
      }
    );

  if (!response.ok) {
    let message =
      `Download failed (${response.status}).`;

    try {
      const payload =
        (await response.json()) as {
          message?: string;
        };

      if (payload.message) {
        message = payload.message;
      }
    } catch {
      // Keep the HTTP fallback.
    }

    throw new Error(message);
  }

  const blob =
    await response.blob();

  const filename =
    filenameFromDisposition(
      response.headers.get(
        "content-disposition"
      ),
      fallbackFilename
    );

  const url =
    URL.createObjectURL(blob);

  try {
    const anchor =
      document.createElement("a");

    anchor.href = url;
    anchor.download = filename;

    document.body.appendChild(
      anchor
    );

    anchor.click();

    anchor.remove();
  } finally {
    URL.revokeObjectURL(url);
  }
}
