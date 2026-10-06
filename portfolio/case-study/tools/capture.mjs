// Takes the screenshots for the case study from a running BuildFlow demo.
// It only reads: it signs in with the demo accounts and looks at pages. It never sends a form.
//
// Environment: BASE_URL (default http://localhost:3000), OUT_DIR (where the PNG files go).
import { chromium } from "playwright";
import fs from "node:fs";
import path from "node:path";

const BASE = (process.env.BASE_URL || "http://localhost:3000").replace(/\/$/, "");
const OUT = process.env.OUT_DIR || path.resolve("screenshots");

fs.mkdirSync(OUT, { recursive: true });

const DESKTOP = { viewport: { width: 1440, height: 900 }, deviceScaleFactor: 1.5, colorScheme: "light", reducedMotion: "reduce" };
const PHONE = {
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2,
  isMobile: true,
  hasTouch: true,
  colorScheme: "light",
  reducedMotion: "reduce",
  userAgent:
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
};

const QUIET_CSS = `
  *, *::before, *::after { animation: none !important; transition: none !important; caret-color: transparent !important; }
  ::-webkit-scrollbar { display: none; }
  html { scrollbar-width: none; }
`;

const results = [];

function log(line) {
  console.log(line);
}

function firstLine(error) {
  return String(error && error.message ? error.message : error).split("\n")[0].slice(0, 220);
}

function sizeOf(file) {
  try {
    return Math.round(fs.statSync(file).size / 1024) + " KB";
  } catch {
    return "?";
  }
}

async function launch() {
  try {
    return await chromium.launch({ channel: "chrome" });
  } catch {
    return await chromium.launch();
  }
}

async function go(page, route) {
  await page.goto(BASE + route, { waitUntil: "domcontentloaded", timeout: 60000 });
  await page.waitForLoadState("networkidle", { timeout: 20000 }).catch(() => {});
  await page.addStyleTag({ content: QUIET_CSS }).catch(() => {});
  await page.waitForTimeout(800);

  if (new URL(page.url()).pathname.startsWith("/login")) {
    throw new Error("sent back to the sign-in page (the session was not accepted)");
  }
}

async function settle(page, ms = 600) {
  await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(ms);
}

async function waitText(page, pattern, timeout = 12000) {
  await page.getByText(pattern).first().waitFor({ state: "visible", timeout });
}

async function save(page, name, options = {}) {
  const file = path.join(OUT, name + ".png");

  await page.screenshot({ path: file, type: "png", ...options });

  return file;
}

async function shot(name, description, body) {
  try {
    const file = await body();

    results.push({ name, description, ok: true, size: sizeOf(file) });
    log("  ok      " + name + "  " + sizeOf(file));
  } catch (error) {
    const reason = String(error && error.message ? error.message : error).split("\n")[0].slice(0, 200);

    results.push({ name, description, ok: false, reason });
    log("  FAILED  " + name + "  " + reason);
  }
}

// ---- sign in with a demo role ------------------------------------------------

async function signIn(browser, options, patterns, label) {
  const context = await browser.newContext(options);
  const page = await context.newPage();

  await page.goto(BASE + "/demo", { waitUntil: "domcontentloaded", timeout: 60000 });
  await page.waitForLoadState("networkidle", { timeout: 20000 }).catch(() => {});

  const links = await page.$$eval('a[href*="/demo/login/"]', (anchors) =>
    anchors.map((a) => ({ href: a.getAttribute("href") || "", text: (a.textContent || "").replace(/\s+/g, " ").trim().slice(0, 70) })),
  );

  if (links.length === 0) {
    await context.close();
    throw new Error("the /demo page has no role links (is the demo turned on? BUILDFLOW_DEMO_ENABLED=true)");
  }

  log("  roles on /demo: " + links.map((l) => l.href.split("/").pop()).join(", "));

  // Match on the address first (a role card's text can mention other roles), then on the text.
  let chosen;

  for (const pattern of patterns) {
    chosen = links.find((l) => pattern.test(l.href));

    if (chosen) break;
  }

  if (!chosen) {
    for (const pattern of patterns) {
      chosen = links.find((l) => pattern.test(l.text));

      if (chosen) break;
    }
  }

  if (!chosen) {
    await context.close();
    throw new Error("no demo role matched " + label);
  }

  await page.goto(BASE + chosen.href, { waitUntil: "domcontentloaded", timeout: 60000 });

  try {
    await page.waitForURL((url) => !url.pathname.startsWith("/demo"), { timeout: 40000 });
  } catch {
    const text = (await page.locator("body").innerText().catch(() => "")).replace(/\s+/g, " ").slice(0, 200);

    await context.close();
    throw new Error("demo sign-in did not finish for " + chosen.href + ": " + text);
  }

  await page.waitForLoadState("networkidle", { timeout: 20000 }).catch(() => {});
  log("  signed in as " + chosen.href.split("/").pop() + " -> " + new URL(page.url()).pathname);

  return { context, page, landing: new URL(page.url()).pathname };
}

async function listProjects(page, prefix = "/app/projects") {
  await go(page, prefix === "/app/projects" ? "/app/projects" : "/app/client");

  const seen = new Map();
  const links = await page.$$eval(`a[href^="${prefix}/"]`, (anchors) =>
    anchors.map((a) => ({ href: a.getAttribute("href") || "", text: (a.textContent || "").replace(/\s+/g, " ").trim() })),
  );
  const pattern = new RegExp("^" + prefix.replace(/\//g, "\\/") + "\\/[0-9a-fA-F-]{36}\\/?$");

  for (const link of links) {
    if (pattern.test(link.href) && !seen.has(link.href)) seen.set(link.href, link.text);
  }

  const list = [...seen.entries()].map(([href, text]) => ({ href: href.replace(/\/$/, ""), name: text }));

  list.sort((a, b) => Number(/rivoli/i.test(b.name)) - Number(/rivoli/i.test(a.name)));

  return list;
}

// ---- the shots -----------------------------------------------------------------

async function desktopShots(browser) {
  log("\nDesktop, signed in as the owner");

  let session;

  try {
    session = await signIn(browser, DESKTOP, [/owner/i, /admin/i], "an owner");
  } catch (error) {
    log("  FAILED  cannot sign in as the owner: " + firstLine(error));
    results.push({ name: "(all desktop shots)", description: "owner sign-in", ok: false, reason: firstLine(error) });

    return;
  }

  const { context, page } = session;
  const projects = await listProjects(page);

  log("  projects: " + (projects.map((p) => p.name || p.href).join(" | ") || "none found"));

  const main = projects[0];
  const withProject = (suffix) => {
    if (!main) throw new Error("no project link found on /app/projects");

    return main.href + suffix;
  };

  await shot("01-dashboard", "Overview with needs-attention and receivables", async () => {
    await go(page, "/app");
    await waitText(page, /needs attention|attention/i).catch(() => {});
    await settle(page, 900);
    await page.evaluate(() => window.scrollTo(0, 0));

    return save(page, "01-dashboard");
  });

  await shot("02-dashboard-full", "Whole overview page", async () => {
    await go(page, "/app");
    await settle(page, 900);

    return save(page, "02-dashboard-full", { fullPage: true });
  });

  await shot("03-search", "Command palette search", async () => {
    await go(page, "/app");
    await page.locator("body").click({ position: { x: 700, y: 450 } }).catch(() => {});

    const palette = page.locator(".uxPalette, [role='dialog']").first();

    await page.keyboard.press("Meta+K");

    if (!(await palette.isVisible({ timeout: 2500 }).catch(() => false))) {
      await page.keyboard.press("Control+K");
    }

    await palette.waitFor({ state: "visible", timeout: 5000 });

    const term = main && main.name ? main.name.split(/\s+/)[0] : "project";

    await page.keyboard.type(term, { delay: 60 });
    await page.locator(".uxPaletteItem, [role='option']").first().waitFor({ state: "visible", timeout: 10000 });
    await settle(page, 500);

    return save(page, "03-search");
  });

  await shot("04-projects", "Project list with filters and progress", async () => {
    await go(page, "/app/projects");
    await settle(page, 800);

    return save(page, "04-projects");
  });

  await shot("05-project-overview", "One project's overview", async () => {
    await go(page, withProject(""));
    await settle(page, 900);

    return save(page, "05-project-overview");
  });

  await shot("06-budget", "Budget with actual, committed and forecast cost", async () => {
    await go(page, withProject("/budget"));
    await settle(page, 900);

    return save(page, "06-budget");
  });

  await shot("07-change-order-impact", "Change order with the impact of approval", async () => {
    const candidates = projects.length ? projects : [];

    for (const project of candidates) {
      await go(page, project.href + "/change-orders");
      await settle(page, 700);

      const impact = page.locator('section[aria-label="Impact of approval"]').first();
      let found = await impact.isVisible({ timeout: 2500 }).catch(() => false);

      if (!found) {
        const rows = page.locator(".commercialList button");
        const count = Math.min(await rows.count(), 8);

        for (let i = 0; i < count && !found; i += 1) {
          await rows.nth(i).click().catch(() => {});
          found = await impact.isVisible({ timeout: 2500 }).catch(() => false);
        }
      }

      if (found) {
        await impact.evaluate((el) => el.scrollIntoView({ block: "center" }));
        await page.waitForTimeout(600);

        return save(page, "07-change-order-impact");
      }
    }

    throw new Error("no pending change order with an impact preview was found in the demo projects");
  });

  await shot("08-timeline", "Project timeline with milestones and Today", async () => {
    await go(page, withProject("/timeline"));
    await page.locator(".uxTimeline").first().waitFor({ state: "visible", timeout: 12000 }).catch(() => {});
    await settle(page, 900);

    return save(page, "08-timeline");
  });

  await shot("09-site-operations", "Site reports, incidents and progress", async () => {
    await go(page, withProject("/site"));
    await settle(page, 900);

    return save(page, "09-site-operations");
  });

  await shot("10-commercial", "Quotes, invoices and payments", async () => {
    await go(page, withProject("/commercial"));
    await settle(page, 900);

    return save(page, "10-commercial");
  });

  await shot("11-tasks", "Tasks", async () => {
    await go(page, withProject("/tasks"));
    await settle(page, 900);

    return save(page, "11-tasks");
  });

  await shot("12-audit-log", "Audit log with filters", async () => {
    await go(page, "/app/audit");
    await page.locator("table tbody tr").first().waitFor({ state: "visible", timeout: 12000 }).catch(() => {});
    await settle(page, 900);

    return save(page, "12-audit-log");
  });

  await context.close();
}

async function clientShots(browser) {
  log("\nDesktop, signed in as the client");

  let session;

  try {
    session = await signIn(browser, DESKTOP, [/client/i, /customer/i], "a client");
  } catch (error) {
    log("  FAILED  cannot sign in as the client: " + firstLine(error));
    results.push({ name: "(client shots)", description: "client sign-in", ok: false, reason: firstLine(error) });

    return;
  }

  const { context, page, landing } = session;

  await shot("13-client-portal", "What the client sees", async () => {
    await go(page, landing || "/app/client");
    await settle(page, 900);

    return save(page, "13-client-portal");
  });

  await shot("14-client-project", "A project in the client portal", async () => {
    const list = await listProjects(page, "/app/client/projects");

    if (!list.length) throw new Error("no project link found in the client portal");

    await go(page, list[0].href);
    await settle(page, 900);

    return save(page, "14-client-project");
  });

  await context.close();
}

async function phoneShots(browser) {
  log("\nPhone (390 x 844), signed in as the site manager");

  let session;

  try {
    session = await signIn(browser, PHONE, [/site/i, /owner/i], "a site manager");
  } catch (error) {
    log("  FAILED  cannot sign in on the phone: " + firstLine(error));
    results.push({ name: "(phone shots)", description: "phone sign-in", ok: false, reason: firstLine(error) });

    return;
  }

  const { context, page } = session;
  const projects = await listProjects(page);
  const main = projects[0];

  await shot("20-phone-overview", "Overview on a phone with the bottom navigation", async () => {
    await go(page, "/app");
    await settle(page, 900);

    return save(page, "20-phone-overview");
  });

  await shot("21-phone-report-basics", "Quick report: the day on site", async () => {
    if (!main) throw new Error("no project link found");

    await go(page, main.href + "/site/capture");
    await waitText(page, /The day on site/i);
    await page.getByRole("button", { name: "Sunny" }).first().click().catch(() => {});
    await page.waitForTimeout(300);

    return save(page, "21-phone-report-basics");
  });

  await shot("22-phone-report-work", "Quick report: what happened today", async () => {
    await page.getByRole("button", { name: "Next" }).click();
    await waitText(page, /What happened today/i);
    await page
      .getByLabel(/^Summary/)
      .fill("Level 2 slab poured and finished. Formwork stripped on level 1. Rebar delivered for level 3.");
    await page.waitForTimeout(300);

    return save(page, "22-phone-report-work");
  });

  await shot("23-phone-report-photos", "Quick report: add photos", async () => {
    await page.getByRole("button", { name: "Next" }).click();
    await waitText(page, /Add photos/i);
    await page.waitForTimeout(300);

    return save(page, "23-phone-report-photos");
  });

  await shot("24-phone-report-review", "Quick report: check before sending", async () => {
    await page.getByRole("button", { name: /Skip photos/i }).click();
    await waitText(page, /Check and send/i);
    await page.waitForTimeout(300);

    // The report is never sent: this browser session is thrown away below.
    return save(page, "24-phone-report-review");
  });

  await context.close();
}

// ---- contact sheet for a quick look at everything ---------------------------------

async function contactSheet(browser) {
  const files = fs.readdirSync(OUT).filter((f) => /^\d\d-.*\.png$/.test(f)).sort();

  if (!files.length) return;

  const html = `<!doctype html><meta charset="utf-8"><style>
    body{margin:0;padding:24px;background:#eef1f5;font:14px system-ui,sans-serif;color:#0f172a}
    .grid{display:grid;grid-template-columns:repeat(4,1fr);gap:18px;align-items:start}
    figure{margin:0;background:#fff;border:1px solid #d8e0ea;border-radius:8px;overflow:hidden}
    img{display:block;width:100%;height:auto}
    figcaption{padding:8px 10px;font-weight:600}
  </style><div class="grid">${files
    .map((f) => `<figure><img src="${f}"><figcaption>${f.replace(/\.png$/, "")}</figcaption></figure>`)
    .join("")}</div>`;
  const sheet = path.join(OUT, "_contact-sheet.html");

  fs.writeFileSync(sheet, html);

  const context = await browser.newContext({ viewport: { width: 1800, height: 1000 } });
  const page = await context.newPage();

  await page.goto("file://" + sheet);
  await page.waitForLoadState("load");
  await page.waitForTimeout(800);
  await page.screenshot({ path: path.join(OUT, "_contact-sheet.jpg"), type: "jpeg", quality: 70, fullPage: true });
  await context.close();
  fs.rmSync(sheet, { force: true });
}

// ---- main ---------------------------------------------------------------------------

const browser = await launch();

log("Capturing from " + BASE + " into " + OUT);

try {
  await desktopShots(browser);
  await clientShots(browser);
  await phoneShots(browser);
  await contactSheet(browser).catch((error) => log("  (contact sheet skipped: " + error.message + ")"));
} finally {
  await browser.close();
}

const okCount = results.filter((r) => r.ok).length;
const failed = results.filter((r) => !r.ok);

log("\n=== RESULT: " + okCount + " screenshots saved, " + failed.length + " failed ===");

for (const r of results) {
  log((r.ok ? "ok      " : "FAILED  ") + r.name + (r.ok ? "  (" + r.size + ")" : "  " + r.reason));
}

process.exit(failed.length ? 2 : 0);
