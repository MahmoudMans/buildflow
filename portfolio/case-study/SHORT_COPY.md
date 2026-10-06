# Short copy for BuildFlow

Ready to paste. Replace anything in [square brackets], and read each line once to make sure you would say it yourself.

## One line (under 100 characters)

A construction project app that shows what a change order costs before anyone approves it.

## Two sentences (project card, portfolio grid)

BuildFlow is a project management app for construction companies, with budgets, schedules, site reports and a client portal. Its change-order impact preview shows the project's figures before and after approval, and a phone-first site report lets a site manager finish the day's report with one hand.

## Fifty words (about page, cover letter)

I built BuildFlow, a multi-tenant project management app for construction companies, from the database to the interface. It has five role-based views, a change-order impact preview, a phone-first site report with draft saving and photo shrinking, and more than 100 backend tests that run against a real PostgreSQL.

## Resume bullets (pick three or four)

- Built BuildFlow, a multi-tenant construction project management app (Spring Boot, PostgreSQL, Next.js, TypeScript) with five role-based views and a seeded demo organization.
- Designed a change-order impact preview, computed by the API, that shows the project's figures before and after approval together with the other pending change orders.
- Shipped a phone-first site report flow with draft saving, in-browser photo shrinking and offline detection.
- Secured the API with opaque session cookies, CSRF protection, login rate limiting, request size limits and tenant checks, and covered the rules with integration tests.
- Wrote more than 100 backend tests against a real PostgreSQL (Testcontainers) and frontend tests with Vitest and Testing Library; evolved the schema through 13 Flyway migrations.

## LinkedIn project entry

**Name:** BuildFlow

**Description:**
A project management app for construction companies. Owners, project managers, site managers, accountants and clients each get their own view of the same organization.

What I'm happiest with:
- A change order shows its effect on the project's figures before anyone approves it.
- A site report that works on a phone: four short steps, a draft saved as you type, and photos shrunk before upload.
- Separate organizations, session cookies, CSRF protection and rate limiting, checked by integration tests against a real database.

Built with Java, Spring Boot, PostgreSQL, MinIO, Next.js, React and TypeScript. Source: https://github.com/MahmoudMans/buildflow

**Skills to tag:** Spring Boot, Java, PostgreSQL, Next.js, React, TypeScript, REST APIs, Docker, Testcontainers, Flyway

## GitHub repository

**About text (350 characters or fewer):**
Project management for construction companies: budgets, change orders with an impact preview, phone-first site reports and a client portal. Spring Boot, PostgreSQL and Next.js.

**Topics:** spring-boot, nextjs, react, typescript, postgresql, flyway, testcontainers, docker-compose, construction, project-management, multi-tenant

**Website field:** [live demo URL]

## Screenshots: alt text and captions

Use the alt text for accessibility and the caption under the image. Files are in `screenshots/`.

| File | Alt text | Caption |
| --- | --- | --- |
| 01-dashboard.png | The BuildFlow dashboard for a construction company owner, listing what needs attention across three projects. | The owner's dashboard. |
| 02-dashboard-full.png | The full dashboard page, captured from top to bottom. | The full dashboard. |
| 03-search.png | The Command-K search palette open over the dashboard. | Search, opened with Command-K. |
| 04-projects.png | The project list with filters and progress. | The project list. |
| 05-project-overview.png | A project overview page showing progress, budget and recent activity. | A project overview, signed in as the owner. |
| 06-budget.png | The budget page, comparing actual, committed and forecast cost. | The budget, with actual, committed and forecast cost. |
| 07-change-order-impact.png | A pending change order with the Impact of approval table showing figures now, after approval and the change. | The impact panel on a pending change order. |
| 08-timeline.png | A project timeline chart. | A project's timeline. |
| 09-site-operations.png | Site operations: daily reports and incidents. | Site operations. |
| 10-commercial.png | The commercial page with quotes, invoices and payments. | Quotes, invoices and payments. |
| 11-tasks.png | The task list for a project. | Tasks. |
| 12-audit-log.png | The audit log table listing who changed what and when. | The audit log. |
| 13-client-portal.png | The client portal home page listing the projects this client can see. | The client portal lists only that client's projects. |
| 14-client-project.png | A single project as the client sees it, with progress and documents. | One project, as the client sees it. |
| 20-phone-overview.png | The project overview on a phone. | Project overview on a phone. |
| 21-phone-report-basics.png | Step one of the quick site report: the basics, such as the weather. | Quick report, step 1: basics. |
| 22-phone-report-work.png | Step two: a summary of the work done. | Quick report, step 2: work done. |
| 23-phone-report-photos.png | Step three: adding photos. | Quick report, step 3: photos. |
| 24-phone-report-review.png | Step four: reviewing the report before sending. | Quick report, step 4: review. |

If a screenshot turns out to show something different from its alt text, fix the alt text to match what is in the picture.

## Questions you may be asked

Short, honest starting points. Open the code behind each one before an interview so you can show it.

- **Why build this?** Change orders are where construction budgets go wrong, and the people deciding rarely see the effect first. Everything else in the app supports that decision.
- **How does the impact preview work?** The API works out the project's figures now and after approval and returns them with any warnings and the other pending change orders. The browser only lays them out.
- **How is tenant data kept apart?** Requests name an organization and the server checks that the signed-in user is a member of it. Integration tests cover that rule against a real database.
- **Why session cookies and not JWTs in local storage?** Opaque session cookies can be revoked on the server, and the token is not kept in browser storage where page scripts can read it. CSRF protection covers the trade-off that cookies bring.
- **What would you change?** Offline completion of site reports with sync, a signature step for change-order approval, and a screen-reader accessibility pass.
- **How did you build it?** [Say how you worked, including any tools you used. The page says "solo project", so change that wording if it is not accurate.]
