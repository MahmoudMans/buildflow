import Link from "next/link";

import {
  portfolioConfig,
} from "../lib/portfolio-config";

const features = [
  {
    index: "01",
    title:
      "Project management",
    description:
      "Projects, teams, milestones, tasks, schedules and operational progress in one workspace.",
  },
  {
    index: "02",
    title:
      "Cost control",
    description:
      "Budgets, expenses, purchase orders, commitments and project cost forecasts.",
  },
  {
    index: "03",
    title:
      "Site operations",
    description:
      "Daily reports, incidents, construction photos and mobile-friendly site workflows.",
  },
  {
    index: "04",
    title:
      "Client commercial",
    description:
      "Quotations, change orders, invoices, payments and outstanding receivables.",
  },
  {
    index: "05",
    title:
      "Analytics",
    description:
      "Organization and project dashboards with health, revenue, expenses and forecast profit.",
  },
  {
    index: "06",
    title:
      "Client portal",
    description:
      "Security-first document visibility, tenant isolation, RBAC and restricted financial exposure.",
  },
];

const stack = [
  "Java 21",
  "Spring Boot",
  "PostgreSQL",
  "Flyway",
  "Next.js",
  "React",
  "TypeScript",
  "MinIO",
  "Mailpit",
  "Docker",
];

export default function HomePage() {
  return (
    <main className="portfolioLanding">
      <nav
        className="portfolioNav"
        aria-label="Public navigation"
      >
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
              Construction Operations
            </small>
          </div>
        </Link>

        <div className="portfolioNavLinks">
          <a href="#features">
            Features
          </a>

          <Link href="/portfolio">
            Architecture
          </Link>

          <Link href="/login">
            Sign in
          </Link>

          <Link
            className="portfolioNavPrimary"
            href="/demo"
          >
            Explore Demo
          </Link>
        </div>
      </nav>

      <section className="portfolioHero">
        <div className="portfolioHeroCopy">
          <span className="portfolioEyebrow">
            CONSTRUCTION PROJECT MANAGEMENT
          </span>

          <h1>
            Build better projects.
            <strong>
              Control every cost.
            </strong>
          </h1>

          <p>
            Manage construction projects,
            budgets, teams, suppliers and
            clients from one professional
            multi-tenant platform.
          </p>

          <div className="portfolioHeroActions">
            <Link
              className="portfolioPrimaryButton"
              href="/demo"
            >
              Explore Demo
            </Link>

            <a
              className="portfolioSecondaryButton"
              href={
                portfolioConfig
                  .githubUrl
              }
              target="_blank"
              rel="noreferrer"
            >
              View Project
            </a>
          </div>

          <div className="portfolioAssurance">
            <span>
              Multi-tenant
            </span>
            <span>
              Role-based access
            </span>
            <span>
              Financial controls
            </span>
            <span>
              Audit-ready
            </span>
          </div>
        </div>

        <div
          className="portfolioProductPreview"
          aria-label="Illustrative BuildFlow project dashboard"
        >
          <header>
            <div>
              <span>
                BF
              </span>

              <strong>
                NorthBuild Construction
              </strong>
            </div>

            <small>
              Project overview
            </small>
          </header>

          <div className="portfolioPreviewBody">
            <aside>
              <strong>
                Overview
              </strong>
              <span>
                Projects
              </span>
              <span>
                Clients
              </span>
              <span>
                Finances
              </span>
              <span>
                Reports
              </span>
            </aside>

            <section>
              <div className="portfolioPreviewTitle">
                <div>
                  <small>
                    PRJ-2026-0001
                  </small>

                  <strong>
                    Rivoli Hotel Renovation
                  </strong>
                </div>

                <span>
                  ON TRACK
                </span>
              </div>

              <div className="portfolioPreviewMetrics">
                <article>
                  <small>
                    CONTRACT
                  </small>
                  <strong>
                    €1.85M
                  </strong>
                </article>

                <article>
                  <small>
                    PROGRESS
                  </small>
                  <strong>
                    62%
                  </strong>
                </article>

                <article>
                  <small>
                    FORECAST
                  </small>
                  <strong>
                    €1.42M
                  </strong>
                </article>

                <article>
                  <small>
                    MARGIN
                  </small>
                  <strong>
                    23.2%
                  </strong>
                </article>
              </div>

              <div className="portfolioPreviewChart">
                {[
                  35,
                  48,
                  42,
                  63,
                  57,
                  76,
                  70,
                  88,
                  82,
                  94,
                ].map(
                  (
                    height,
                    index
                  ) => (
                    <span
                      key={
                        index
                      }
                      style={{
                        height:
                          `${height}%`,
                      }}
                    />
                  )
                )}
              </div>
            </section>
          </div>
        </div>
      </section>

      <section
        className="portfolioProofStrip"
        aria-label="BuildFlow capabilities"
      >
        <div>
          <strong>
            15
          </strong>
          <span>
            delivery phases
          </span>
        </div>

        <div>
          <strong>
            7
          </strong>
          <span>
            organization roles
          </span>
        </div>

        <div>
          <strong>
            100%
          </strong>
          <span>
            backend-authoritative finance
          </span>
        </div>

        <div>
          <strong>
            0
          </strong>
          <span>
            paid external APIs
          </span>
        </div>
      </section>

      <section
        id="features"
        className="portfolioSection"
      >
        <header className="portfolioSectionHeader">
          <span>
            BUILT FOR THE FULL PROJECT LIFECYCLE
          </span>

          <h2>
            From the first client conversation
            to final project reporting.
          </h2>

          <p>
            BuildFlow combines operational,
            commercial and financial project
            management without turning the
            product into a generic CRUD
            dashboard.
          </p>
        </header>

        <div className="portfolioFeatureGrid">
          {features.map(
            (feature) => (
              <article
                key={
                  feature.index
                }
              >
                <span>
                  {feature.index}
                </span>

                <h3>
                  {feature.title}
                </h3>

                <p>
                  {
                    feature.description
                  }
                </p>
              </article>
            )
          )}
        </div>
      </section>

      <section className="portfolioDarkSection">
        <div>
          <span className="portfolioEyebrow">
            ARCHITECTED AS A REAL B2B SAAS
          </span>

          <h2>
            Secure boundaries.
            Authoritative calculations.
            Traceable decisions.
          </h2>

          <p>
            UUID identifiers, tenant-scoped
            persistence, backend RBAC,
            controlled state transitions,
            audit logs and secure object
            storage are part of the core
            architecture rather than
            frontend conventions.
          </p>

          <Link
            className="portfolioDarkLink"
            href="/portfolio"
          >
            Explore the architecture
          </Link>
        </div>

        <div className="portfolioArchitectureMini">
          <span>
            Next.js
          </span>
          <b>
            ↓
          </b>
          <span>
            Spring Boot REST API
          </span>
          <b>
            ↓
          </b>

          <div>
            <span>
              PostgreSQL
            </span>
            <span>
              MinIO
            </span>
            <span>
              Mailpit
            </span>
          </div>
        </div>
      </section>

      <section className="portfolioSection">
        <header className="portfolioSectionHeader compact">
          <span>
            TECHNOLOGY
          </span>

          <h2>
            Production-style tooling
            without paid platform APIs.
          </h2>
        </header>

        <div className="portfolioStack">
          {stack.map(
            (technology) => (
              <span key={technology}>
                {technology}
              </span>
            )
          )}
        </div>
      </section>

      <section className="portfolioFinalCta">
        <span>
          EXPERIENCE BUILDFLOW
        </span>

        <h2>
          Explore a populated construction
          company from five different roles.
        </h2>

        <p>
          The demo includes an Owner,
          Project Manager, Site Manager,
          Accountant and restricted Client
          view.
        </p>

        <Link
          className="portfolioPrimaryButton"
          href="/demo"
        >
          Open the demo
        </Link>
      </section>

      <footer className="portfolioFooter">
        <div>
          <strong>
            BuildFlow
          </strong>
          <span>
            Construction Project Management Platform
          </span>
        </div>

        <div>
          <Link href="/portfolio">
            Architecture
          </Link>

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
      </footer>
    </main>
  );
}
