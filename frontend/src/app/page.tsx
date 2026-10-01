import Link from "next/link";
import styles from "./page.module.css";

const capabilities = [
  {
    title: "Project management",
    description:
      "Plan projects, assign teams and follow tasks, milestones and schedules from a shared workspace.",
  },
  {
    title: "Cost control",
    description:
      "Bring budgets, expenses, purchase orders and commitments together to review project costs and forecasts.",
  },
  {
    title: "Site operations",
    description:
      "Keep daily reports, incidents, construction photos and progress updates connected to each project.",
  },
  {
    title: "Commercial management",
    description:
      "Follow quotations, change orders, invoices, payments and outstanding balances across the client relationship.",
  },
  {
    title: "Project insights",
    description:
      "Review project health, revenue, expenses and forecast profit across organization and project dashboards.",
  },
  {
    title: "Client portal",
    description:
      "Give clients access to explicitly shared project information through a separate, restricted view.",
  },
] as const;

const roles = [
  {
    title: "Owner",
    description:
      "Review company-wide projects, finances and administration.",
  },
  {
    title: "Project Manager",
    description:
      "Explore planning, execution and financial visibility for assigned projects.",
  },
  {
    title: "Site Manager",
    description:
      "Review site reports, incidents and project progress.",
  },
  {
    title: "Accountant",
    description:
      "Explore invoices, payments, expenses and financial reporting.",
  },
  {
    title: "Client",
    description:
      "View project information explicitly shared with the client.",
  },
] as const;

const architectureLayers = [
  {
    label: "Application interface",
    technologies: "Next.js 16.3.3 · React 19.2 · TypeScript",
    responsibility:
      "Presents the public experience and authenticated construction workflows.",
  },
  {
    label: "Application API",
    technologies: "Spring Boot 4.1.1 · Java 21",
    responsibility:
      "Coordinates application rules, access control, calculations and workflow operations.",
  },
  {
    label: "Data and file storage",
    technologies: "PostgreSQL 17 · MinIO",
    responsibility:
      "Stores application records and project file assets in distinct persistence layers.",
  },
] as const;

const supportingTools = [
  {
    title: "Flyway",
    description: "Database schema migrations",
  },
  {
    title: "Docker",
    description: "Repeatable local runtime",
  },
  {
    title: "Mailpit",
    description: "Development email testing",
  },
] as const;

export default function HomePage() {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={`${styles.container} ${styles.headerInner}`}>
          <Link href="/" className={styles.brand} aria-label="BuildFlow home">
            <span className={styles.brandMark} aria-hidden="true">
              BF
            </span>

            <span className={styles.brandText}>
              <strong>BuildFlow</strong>
              <span>Construction operations</span>
            </span>
          </Link>

          <nav className={styles.desktopNav} aria-label="Primary navigation">
            <a href="#features" className={styles.navLink}>
              Features
            </a>

            <Link href="/portfolio" className={styles.navLink}>
              Architecture
            </Link>

            <Link href="/login" className={styles.navLink}>
              Sign in
            </Link>

            <Link href="/demo" className={styles.primaryButtonSmall}>
              Explore demo
            </Link>
          </nav>

          <details className={styles.mobileMenu}>
            <summary>Menu</summary>

            <nav className={styles.mobileNav} aria-label="Mobile navigation">
              <a href="#features">Features</a>
              <Link href="/portfolio">Architecture</Link>
              <Link href="/login">Sign in</Link>
              <Link href="/demo" className={styles.mobilePrimaryAction}>
                Explore demo
              </Link>
            </nav>
          </details>
        </div>
      </header>

      <main>
        <section className={styles.hero} aria-labelledby="hero-title">
          <div className={`${styles.container} ${styles.heroGrid}`}>
            <div className={styles.heroCopy}>
              <p className={styles.eyebrow}>Construction operations</p>

              <h1 id="hero-title" className={styles.heroTitle}>
                Projects in sync.{" "}
                <span className={styles.heroAccent}>Costs in view.</span>
              </h1>

              <p className={styles.heroDescription}>
                BuildFlow brings project planning, site reporting and financial
                workflows into one construction management workspace.
              </p>

              <div className={styles.heroActions}>
                <Link href="/demo" className={styles.primaryButton}>
                  Explore demo
                </Link>

                <a
                  href="https://github.com/MahmoudMans/buildflow"
                  target="_blank"
                  rel="noreferrer"
                  className={styles.secondaryButton}
                  aria-label="View source on GitHub, opens in a new tab"
                >
                  View source on GitHub
                  <span aria-hidden="true">↗</span>
                </a>
              </div>

              <p className={styles.helperText}>
                <span className={styles.helperDot} aria-hidden="true" />
                Explore a sample construction company through five demo roles.
              </p>
            </div>

            <figure
              className={styles.projectPreview}
              aria-labelledby="preview-title"
            >
              <div className={styles.previewTop}>
                <div>
                  <div className={styles.organizationRow}>
                    <span className={styles.organizationName}>
                      NorthBuild Construction
                    </span>

                    <span className={styles.sampleBadge}>Sample data</span>
                  </div>

                  <p className={styles.previewKicker}>
                    Construction project overview
                  </p>
                </div>

                <span className={styles.previewMark} aria-hidden="true">
                  BF
                </span>
              </div>

              <div className={styles.previewProject}>
                <p className={styles.previewLabel}>Current project</p>

                <h2 id="preview-title">Rivoli Hotel Renovation</h2>
              </div>

              <div className={styles.summaryGrid}>
                <div className={styles.summaryItem}>
                  <span className={styles.summaryLabel}>Contract value</span>
                  <strong className={styles.summaryValue}>€1.85M</strong>
                </div>

                <div className={styles.summaryItem}>
                  <div className={styles.progressHeading}>
                    <span className={styles.summaryLabel}>Progress</span>
                    <strong className={styles.progressValue}>62%</strong>
                  </div>

                  <div
                    className={styles.progressTrack}
                    role="progressbar"
                    aria-label="Illustrative project progress"
                    aria-valuemin={0}
                    aria-valuemax={100}
                    aria-valuenow={62}
                  >
                    <span className={styles.progressFill} />
                  </div>
                </div>
              </div>

              <figcaption className={styles.previewCaption}>
                Illustrative project overview. Open the demo to inspect the
                workflows.
              </figcaption>
            </figure>
          </div>
        </section>

        <section
          id="features"
          className={styles.featuresSection}
          aria-labelledby="features-title"
        >
          <div className={styles.container}>
            <div className={styles.sectionIntro}>
              <p className={styles.eyebrow}>Functional scope</p>

              <h2 id="features-title" className={styles.sectionTitle}>
                Plan, deliver and report.
              </h2>

              <p className={styles.sectionDescription}>
                Explore the operational, commercial and financial areas included
                in the BuildFlow demo.
              </p>
            </div>

            <div className={styles.featureGrid}>
              {capabilities.map((capability) => (
                <article className={styles.featureCard} key={capability.title}>
                  <h3>{capability.title}</h3>
                  <p>{capability.description}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section
          className={styles.rolesSection}
          aria-labelledby="roles-title"
        >
          <div className={`${styles.container} ${styles.rolesLayout}`}>
            <div className={styles.rolesIntro}>
              <p className={styles.eyebrow}>Demo perspectives</p>

              <h2 id="roles-title" className={styles.sectionTitle}>
                One company. Different responsibilities.
              </h2>

              <p className={styles.sectionDescription}>
                Explore NorthBuild Construction as an Owner, Project Manager,
                Site Manager, Accountant or Client.
              </p>
            </div>

            <div className={styles.roleContent}>
              <div className={styles.roleList}>
                {roles.map((role) => (
                  <div className={styles.roleRow} key={role.title}>
                    <h3>{role.title}</h3>
                    <p>{role.description}</p>
                  </div>
                ))}
              </div>

              <div className={styles.roleActions}>
                <Link href="/demo" className={styles.primaryButton}>
                  Explore demo
                </Link>

                <div className={styles.roleHelper}>
                  <span>Choose a role on the next page.</span>
                  <span className={styles.sampleReminder}>
                    Use sample data only.
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section
          id="architecture"
          className={styles.architectureSection}
          aria-labelledby="architecture-title"
        >
          <div className={`${styles.container} ${styles.architectureGrid}`}>
            <div className={styles.architectureCopy}>
              <p className={styles.inverseEyebrow}>Engineering</p>

              <h2
                id="architecture-title"
                className={styles.inverseSectionTitle}
              >
                Inside the BuildFlow architecture.
              </h2>

              <p className={styles.inverseDescription}>
                Follow the application layers behind the demo, from the
                interface to the API, data storage and supporting tools.
              </p>

              <p className={styles.inverseDescriptionSecondary}>
                Explore the design decisions around tenant isolation, access
                control, financial calculations and audit records.
              </p>

              <Link href="/portfolio" className={styles.inverseButton}>
                View architecture
              </Link>
            </div>

            <div className={styles.architectureDiagram}>
              <div className={styles.layerStack}>
                {architectureLayers.map((layer, index) => (
                  <div key={layer.label}>
                    <div className={styles.layerNode}>
                      <p className={styles.layerLabel}>{layer.label}</p>
                      <h3>{layer.technologies}</h3>
                      <p>{layer.responsibility}</p>
                    </div>

                    {index < architectureLayers.length - 1 ? (
                      <div
                        className={styles.layerConnector}
                        aria-hidden="true"
                      >
                        ↓
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>

              <div className={styles.supportingTools}>
                <p className={styles.supportingToolsLabel}>
                  Supporting implementation tools
                </p>

                <div className={styles.supportingToolsGrid}>
                  {supportingTools.map((tool) => (
                    <div className={styles.toolItem} key={tool.title}>
                      <strong>{tool.title}</strong>
                      <span>{tool.description}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section
          className={styles.finalSection}
          aria-labelledby="final-title"
        >
          <div className={styles.container}>
            <div className={styles.finalBand}>
              <div className={styles.finalCopy}>
                <h2 id="final-title">
                  Explore the work. Inspect the implementation.
                </h2>

                <p>
                  Open the role-based demo, or review the architecture behind it.
                </p>
              </div>

              <div className={styles.finalActions}>
                <Link href="/demo" className={styles.primaryButton}>
                  Explore demo
                </Link>

                <Link href="/portfolio" className={styles.secondaryButton}>
                  View architecture
                </Link>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={`${styles.container} ${styles.footerInner}`}>
          <div className={styles.footerBrand}>
            <strong>BuildFlow</strong>
            <span>Construction management · Portfolio demo</span>
          </div>

          <nav className={styles.footerNav} aria-label="Footer navigation">
            <a href="#features">Features</a>
            <Link href="/demo">Demo</Link>
            <Link href="/portfolio">Architecture</Link>
            <a
              href="https://github.com/MahmoudMans/buildflow"
              target="_blank"
              rel="noreferrer"
              aria-label="GitHub repository, opens in a new tab"
            >
              GitHub ↗
            </a>
          </nav>
        </div>
      </footer>
    </div>
  );
}
