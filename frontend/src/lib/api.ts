import type { ApiErrorBody } from "./types";

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080/api/v1";

let csrfToken: string | null = null;

export class ApiClientError extends Error {
  status: number;
  code: string;

  constructor(
    status: number,
    code: string,
    message: string
  ) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

async function ensureCsrfToken(
  force = false
): Promise<string> {
  if (csrfToken && !force) {
    return csrfToken;
  }

  const response = await fetch(
    `${API_BASE}/auth/csrf`,
    {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new ApiClientError(
      response.status,
      "CSRF_INITIALIZATION_FAILED",
      "Could not initialize request security."
    );
  }

  const body = (await response.json()) as {
    token: string;
  };

  csrfToken = body.token;

  return body.token;
}

async function parseError(
  response: Response
): Promise<ApiClientError> {
  let body: ApiErrorBody = {};

  try {
    body = (await response.json()) as ApiErrorBody;
  } catch {
    body = {};
  }

  return new ApiClientError(
    response.status,
    body.code ?? "REQUEST_FAILED",
    body.message ?? "The request failed."
  );
}

async function refreshSession(): Promise<boolean> {
  try {
    const token =
      await ensureCsrfToken();

    const response = await fetch(
      `${API_BASE}/auth/refresh`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "X-XSRF-TOKEN": token,
        },
      }
    );

    return response.ok;
  } catch {
    return false;
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  allowRefresh = true
): Promise<T> {
  const method =
    (options.method ?? "GET").toUpperCase();

  const headers =
    new Headers(options.headers);

  if (
    options.body &&
    !headers.has("Content-Type")
  ) {
    headers.set(
      "Content-Type",
      "application/json"
    );
  }

  if (
    !["GET", "HEAD", "OPTIONS"].includes(
      method
    )
  ) {
    const token =
      await ensureCsrfToken();

    headers.set(
      "X-XSRF-TOKEN",
      token
    );
  }

  const response = await fetch(
    `${API_BASE}${path}`,
    {
      ...options,
      method,
      headers,
      credentials: "include",
      cache: "no-store",
    }
  );

  if (
    response.status === 401 &&
    allowRefresh &&
    path !== "/auth/login" &&
    path !== "/auth/register" &&
    path !== "/auth/refresh"
  ) {
    const refreshed =
      await refreshSession();

    if (refreshed) {
      return apiRequest<T>(
        path,
        options,
        false
      );
    }
  }

  if (!response.ok) {
    throw await parseError(
      response
    );
  }

  if (
    response.status === 204
  ) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function apiGet<T>(
  path: string,
  organizationId?: string
) {
  const headers =
    new Headers();

  if (organizationId) {
    headers.set(
      "X-Organization-Id",
      organizationId
    );
  }

  return apiRequest<T>(
    path,
    {
      method: "GET",
      headers,
    }
  );
}

export function apiPost<T>(
  path: string,
  body?: unknown,
  organizationId?: string
) {
  const headers =
    new Headers();

  if (organizationId) {
    headers.set(
      "X-Organization-Id",
      organizationId
    );
  }

  return apiRequest<T>(
    path,
    {
      method: "POST",
      headers,
      body:
        body === undefined
          ? undefined
          : JSON.stringify(body),
    }
  );
}

export function apiPatch<T>(
  path: string,
  body: unknown,
  organizationId?: string
) {
  const headers =
    new Headers();

  if (organizationId) {
    headers.set(
      "X-Organization-Id",
      organizationId
    );
  }

  return apiRequest<T>(
    path,
    {
      method: "PATCH",
      headers,
      body: JSON.stringify(body),
    }
  );
}

export function apiDelete<T>(
  path: string,
  organizationId?: string
) {
  const headers =
    new Headers();

  if (organizationId) {
    headers.set(
      "X-Organization-Id",
      organizationId
    );
  }

  return apiRequest<T>(
    path,
    {
      method: "DELETE",
      headers,
    }
  );
}
