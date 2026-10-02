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
      <nav className="demoTopbar" aria-label="Portfolio navigation">
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

        <div
          className="portfolioDocsNav"
        >
          <div
            className="portfolioDocsAnchors"
            aria-label="Architecture sections"
          >
            <a href="#flow">
              Flow
            </a>

            <a href="#workflow">
              Workflow
            </a>

            <a href="#modules">
              Modules
            </a>

            <a href="#security">
              Security
            </a>

            <a href="#deployment">
              Local setup
            </a>
          </div>

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
          BuildFlow separates presentation,
          backend business rules, tenant
          authorization, relational data,
          file storage and supporting
          infrastructure into explicit
          system boundaries.
        </p>
      </header>

      <section className="portfolioDocsSection" id="flow">
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

          <div
          className="portfolioFlowConnector"
          aria-label="Request boundary"
        >
          <span>
            REST / HTTP-only session
          </span>

          <b
            aria-hidden="true"
          >
            ↓
          </b>
        </div>

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

          <div
          className="portfolioFlowConnector"
          aria-label="Persistence boundary"
        >
          <span>
            Persistence boundaries
          </span>

          <b
            aria-hidden="true"
          >
            ↓
          </b>
        </div>

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

      <section className="portfolioDocsSection" id="workflow">
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

      <section className="portfolioDocsSection" id="modules">
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

      <section className="portfolioDocsSection" id="security">
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

      <section className="portfolioDocsSection" id="deployment">
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

      <section
        id="demo"
        className="portfolioDocsFinal"
      >
        <span
          className="portfolioEyebrow portfolioDocsFinalEyebrow"
        >
          EXPERIENCE BUILDFLOW
        </span>

        <h2>
          See how the architecture behaves
          from the perspective of a real user.
        </h2>

        <p>
          Move from the architecture model
          into the role-based BuildFlow demo.
        </p>

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
