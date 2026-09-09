import Link from "next/link";

import {
  portfolioConfig,
} from "../../lib/portfolio-config";

const lifecycle = [
  "Organization",
  "Client",
  "Project",
  "Planning",
  "Budget",
  "Tasks & Milestones",
  "Procurement",
  "Site Execution",
  "Expenses",
  "Client Invoicing",
  "Completion",
  "Financial Analysis",
];

const modules = [
  "Authentication & Organizations",
  "Clients & Projects",
  "Tasks & Milestones",
  "Budget & Expenses",
  "Procurement",
  "Quotes / Invoices / Payments",
  "Change Orders",
  "Site Operations",
  "Documents & Client Portal",
  "Notifications & Audit",
  "Dashboards & Analytics",
  "Reporting",
  "Security Hardening",
  "UX & Accessibility",
];

export default function PortfolioPage() {
  return (
    <main className="portfolioDocumentation">
      <nav className="demoTopbar">
        <Link
          className="portfolioBrand"
          href="/"
        >
          <span>
            BF
          </span>

          <div>
            <strong>
              BuildFlow
            </strong>

            <small>
              Architecture
            </small>
          </div>
        </Link>

        <div>
          <Link href="/demo">
            Demo
          </Link>

          <a
            href={
              portfolioConfig
                .githubUrl
            }
            target="_blank"
            rel="noreferrer"
          >
            GitHub
          </a>
        </div>
      </nav>

      <header className="portfolioDocsHero">
        <span className="portfolioEyebrow">
          VERSION 1 ARCHITECTURE
        </span>

        <h1>
          A production-style vertical SaaS
          architecture for construction
          operations.
        </h1>

        <p>
          BuildFlow separates frontend
          presentation, authoritative
          backend business rules, tenant
          security, relational persistence,
          object storage and infrastructure
          services.
        </p>
      </header>

      <section className="portfolioDocsSection">
        <header>
          <span>
            SYSTEM ARCHITECTURE
          </span>

          <h2>
            Request and data flow
          </h2>
        </header>

        <div
          className="portfolioArchitectureDiagram"
          aria-label="BuildFlow architecture diagram"
        >
          <article>
            <small>
              PRESENTATION
            </small>
            <strong>
              Next.js 16
            </strong>
            <span>
              React · TypeScript
            </span>
          </article>

          <b>
            REST / HTTP-only session
          </b>

          <article>
            <small>
              APPLICATION
            </small>
            <strong>
              Spring Boot
            </strong>
            <span>
              Business rules · RBAC · Tenant isolation
            </span>
          </article>

          <b>
            Persistence boundaries
          </b>

          <div>
            <article>
              <small>
                DATA
              </small>
              <strong>
                PostgreSQL
              </strong>
              <span>
                Flyway-managed relational model
              </span>
            </article>

            <article>
              <small>
                FILES
              </small>
              <strong>
                MinIO
              </strong>
              <span>
                S3-compatible secure object storage
              </span>
            </article>

            <article>
              <small>
                EMAIL
              </small>
              <strong>
                Mailpit
              </strong>
              <span>
                Local development email workflow
              </span>
            </article>
          </div>
        </div>
      </section>

      <section className="portfolioDocsSection">
        <header>
          <span>
            PRODUCT LIFECYCLE
          </span>

          <h2>
            One connected construction
            workflow
          </h2>
        </header>

        <div className="portfolioLifecycle">
          {lifecycle.map(
            (
              item,
              index
            ) => (
              <div key={item}>
                <span>
                  {String(
                    index + 1
                  ).padStart(
                    2,
                    "0"
                  )}
                </span>

                <strong>
                  {item}
                </strong>
              </div>
            )
          )}
        </div>
      </section>

      <section className="portfolioDocsSection">
        <header>
          <span>
            MODULES
          </span>

          <h2>
            Feature-oriented architecture
          </h2>
        </header>

        <div className="portfolioModuleGrid">
          {modules.map(
            (
              module,
              index
            ) => (
              <article
                key={module}
              >
                <span>
                  {String(
                    index + 1
                  ).padStart(
                    2,
                    "0"
                  )}
                </span>

                <strong>
                  {module}
                </strong>
              </article>
            )
          )}
        </div>
      </section>

      <section className="portfolioDocsSection">
        <header>
          <span>
            SECURITY MODEL
          </span>

          <h2>
            Backend-enforced access
          </h2>
        </header>

        <div className="portfolioSecurityCards">
          <article>
            <strong>
              Authentication
            </strong>
            <p>
              Server-side session handling
              with secure cookie controls and
              login rate limiting.
            </p>
          </article>

          <article>
            <strong>
              Tenant isolation
            </strong>
            <p>
              Every business record belongs
              to one organization and
              protected requests validate
              membership.
            </p>
          </article>

          <article>
            <strong>
              Project access
            </strong>
            <p>
              Project membership and role
              checks prevent identifier-based
              cross-project access.
            </p>
          </article>

          <article>
            <strong>
              File security
            </strong>
            <p>
              Files are stored by opaque
              object keys and downloaded only
              after backend authorization.
            </p>
          </article>

          <article>
            <strong>
              Financial authority
            </strong>
            <p>
              Totals and forecasts are
              calculated on the backend using
              BigDecimal—not trusted from the
              browser.
            </p>
          </article>

          <article>
            <strong>
              Auditability
            </strong>
            <p>
              Important mutations generate
              durable activity and audit
              evidence.
            </p>
          </article>
        </div>
      </section>

      <section className="portfolioDocsSection">
        <header>
          <span>
            LOCAL DEPLOYMENT
          </span>

          <h2>
            Reproducible Docker environment
          </h2>
        </header>

        <div className="portfolioDeployment">
          <article>
            <span>
              01
            </span>

            <div>
              <strong>
                Build and start
              </strong>

              <code>
                docker compose up -d --build
              </code>
            </div>
          </article>

          <article>
            <span>
              02
            </span>

            <div>
              <strong>
                Frontend
              </strong>

              <code>
                http://localhost:3001
              </code>
            </div>
          </article>

          <article>
            <span>
              03
            </span>

            <div>
              <strong>
                Backend health
              </strong>

              <code>
                http://localhost:8080/actuator/health
              </code>
            </div>
          </article>

          <article>
            <span>
              04
            </span>

            <div>
              <strong>
                Infrastructure
              </strong>

              <code>
                PostgreSQL · MinIO · Mailpit
              </code>
            </div>
          </article>
        </div>
      </section>

      <section className="portfolioDocsFinal">
        <h2>
          See the architecture from the
          perspective of a real user.
        </h2>

        <Link
          className="portfolioPrimaryButton"
          href="/demo"
        >
          Explore role-based demo
        </Link>
      </section>
    </main>
  );
}
