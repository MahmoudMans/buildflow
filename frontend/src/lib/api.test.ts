import { afterEach, describe, expect, it, vi } from "vitest";

const BASE =
  process.env.NEXT_PUBLIC_API_BASE_URL ??
  "http://localhost:8080/api/v1";

type Handler = (
  path: string,
  init: RequestInit,
) => Response | Promise<Response>;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function pause(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms);
  });
}

/** Replaces fetch with a handler keyed by path (the API base is removed). */
function install(handler: Handler) {
  const calls: { url: string; init: RequestInit }[] = [];

  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      const options = init ?? {};

      calls.push({ url, init: options });

      return handler(url.replace(BASE, ""), options);
    }),
  );

  return {
    calls,
    count: (path: string) =>
      calls.filter((call) => call.url === `${BASE}${path}`).length,
    to: (path: string) =>
      calls.filter((call) => call.url === `${BASE}${path}`),
  };
}

/** Fresh module state (CSRF token, in-flight refresh) for every test. */
async function loadApi() {
  vi.resetModules();

  return import("./api");
}

function header(init: RequestInit, name: string) {
  return new Headers(init.headers).get(name);
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("api client", () => {
  it("sends the organization header and parses the JSON body on GET", async () => {
    const net = install(() => json({ ok: true }));
    const { apiGet } = await loadApi();

    const result = await apiGet<{ ok: boolean }>("/things", "org-1");

    expect(result).toEqual({ ok: true });
    expect(net.calls).toHaveLength(1);
    expect(header(net.calls[0].init, "X-Organization-Id")).toBe("org-1");
    expect(net.calls[0].init.credentials).toBe("include");
    expect(net.count("/auth/csrf")).toBe(0);
  });

  it("omits the organization header when none is given", async () => {
    const net = install(() => json({}));
    const { apiGet } = await loadApi();

    await apiGet("/things");

    expect(header(net.calls[0].init, "X-Organization-Id")).toBeNull();
  });

  it("fetches the CSRF token once and reuses it for later writes", async () => {
    const net = install((path) =>
      path === "/auth/csrf" ? json({ token: "csrf-1" }) : json({ id: 1 }),
    );
    const { apiPost } = await loadApi();

    await apiPost("/things", { a: 1 }, "org-1");
    await apiPost("/things", { a: 2 }, "org-1");

    expect(net.count("/auth/csrf")).toBe(1);

    const posts = net.to("/things");

    expect(posts).toHaveLength(2);

    for (const post of posts) {
      expect(header(post.init, "X-XSRF-TOKEN")).toBe("csrf-1");
      expect(header(post.init, "Content-Type")).toBe("application/json");
      expect(header(post.init, "X-Organization-Id")).toBe("org-1");
    }

    expect(posts[0].init.body).toBe(JSON.stringify({ a: 1 }));
  });

  it("lets concurrent writes share a single CSRF request", async () => {
    const net = install(async (path) => {
      if (path === "/auth/csrf") {
        await pause(10);

        return json({ token: "csrf-1" });
      }

      return json({});
    });
    const { apiPost } = await loadApi();

    await Promise.all([apiPost("/a"), apiPost("/b"), apiPost("/c")]);

    expect(net.count("/auth/csrf")).toBe(1);

    for (const path of ["/a", "/b", "/c"]) {
      expect(header(net.to(path)[0].init, "X-XSRF-TOKEN")).toBe("csrf-1");
    }
  });

  it("refreshes an expired session once and retries the request", async () => {
    let attempts = 0;

    const net = install((path) => {
      if (path === "/auth/csrf") {
        return json({ token: "t" });
      }

      if (path === "/auth/refresh") {
        return json({});
      }

      attempts += 1;

      return attempts === 1
        ? json({ code: "UNAUTHORIZED", message: "expired" }, 401)
        : json({ value: 42 });
    });
    const { apiGet } = await loadApi();

    await expect(apiGet<{ value: number }>("/me")).resolves.toEqual({
      value: 42,
    });

    expect(net.count("/auth/refresh")).toBe(1);
    expect(net.count("/me")).toBe(2);
    expect(header(net.to("/auth/refresh")[0].init, "X-XSRF-TOKEN")).toBe("t");
  });

  it("makes requests that expire together share one refresh", async () => {
    const seen = new Map<string, number>();

    const net = install(async (path) => {
      if (path === "/auth/csrf") {
        return json({ token: "t" });
      }

      if (path === "/auth/refresh") {
        await pause(20);

        return json({});
      }

      const attempt = (seen.get(path) ?? 0) + 1;

      seen.set(path, attempt);

      return attempt === 1 ? json({}, 401) : json({ path });
    });
    const { apiGet } = await loadApi();

    const results = await Promise.all([
      apiGet<{ path: string }>("/a"),
      apiGet<{ path: string }>("/b"),
      apiGet<{ path: string }>("/c"),
    ]);

    expect(results.map((result) => result.path)).toEqual(["/a", "/b", "/c"]);
    expect(net.count("/auth/refresh")).toBe(1);
    expect(net.count("/auth/csrf")).toBe(1);
  });

  it("gives up when the refresh itself fails", async () => {
    const net = install((path) => {
      if (path === "/auth/csrf") {
        return json({ token: "t" });
      }

      if (path === "/auth/refresh") {
        return json({ code: "SESSION_EXPIRED", message: "Sign in again." }, 401);
      }

      return json({ code: "UNAUTHORIZED", message: "expired" }, 401);
    });
    const { apiGet } = await loadApi();

    await expect(apiGet("/me")).rejects.toMatchObject({
      status: 401,
      code: "UNAUTHORIZED",
    });

    expect(net.count("/me")).toBe(1);
    expect(net.count("/auth/refresh")).toBe(1);
  });

  it("does not try to refresh when logging in fails", async () => {
    const net = install((path) =>
      path === "/auth/csrf"
        ? json({ token: "t" })
        : json({ code: "INVALID_CREDENTIALS", message: "Wrong password." }, 401),
    );
    const { apiPost } = await loadApi();

    await expect(
      apiPost("/auth/login", { email: "a@b.test", password: "x" }),
    ).rejects.toMatchObject({ status: 401, code: "INVALID_CREDENTIALS" });

    expect(net.count("/auth/refresh")).toBe(0);
  });

  it("turns error bodies into ApiClientError with a fallback for non-JSON", async () => {
    install((path) =>
      path === "/conflict"
        ? json({ code: "CONFLICT", message: "Already exists" }, 409)
        : new Response("<html>oops</html>", { status: 500 }),
    );
    const { apiGet, ApiClientError } = await loadApi();

    const conflict = await apiGet("/conflict").catch((error: unknown) => error);

    expect(conflict).toBeInstanceOf(ApiClientError);
    expect(conflict).toMatchObject({
      status: 409,
      code: "CONFLICT",
      message: "Already exists",
    });

    const broken = await apiGet("/broken").catch((error: unknown) => error);

    expect(broken).toMatchObject({
      status: 500,
      code: "REQUEST_FAILED",
      message: "The request failed.",
    });
  });

  it("returns undefined for 204 responses and sends the CSRF token on DELETE", async () => {
    const net = install((path) =>
      path === "/auth/csrf"
        ? json({ token: "csrf-9" })
        : new Response(null, { status: 204 }),
    );
    const { apiDelete } = await loadApi();

    await expect(apiDelete("/things/1", "org-1")).resolves.toBeUndefined();

    const sent = net.to("/things/1")[0];

    expect(sent.init.method).toBe("DELETE");
    expect(header(sent.init, "X-XSRF-TOKEN")).toBe("csrf-9");
    expect(header(sent.init, "X-Organization-Id")).toBe("org-1");
  });
});
