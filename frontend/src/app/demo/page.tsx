import Link from "next/link";

import styles from "./demo.module.css";

import {
  demoAccounts,
  portfolioConfig,
} from "../../lib/portfolio-config";

export default function DemoPage() {
  return (
    <main className={`demoExperience ${styles.demoPage}`}>
      <nav className="demoTopbar" aria-label="Demo navigation">
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
              Portfolio Demo
            </small>
          </div>
        </Link>

        <Link href="/">
          Back to BuildFlow
        </Link>
      </nav>

      <section className="demoHero">
        <span className="portfolioEyebrow">
          INTERACTIVE ROLE-BASED DEMO
        </span>

        <h1>
          Explore BuildFlow from five
          working perspectives.
        </h1>

        <p>
          NorthBuild Construction is
          pre-populated with three realistic
          construction projects. Choose a
          role to enter the demo.
        </p>

        <div className="demoOrganizationBadge">
          <strong>
            {
              portfolioConfig
                .demoOrganization
            }
          </strong>

          <span>
            3 demo projects
          </span>
        </div>
      </section>

      <section
        className={
          styles.roleSection
        }
        aria-labelledby="demo-role-heading"
      >
        <header
          className={
            styles.roleSectionHeader
          }
        >
          <h2
            id="demo-role-heading"
          >
            Choose a role
          </h2>

          <p>
            Each role opens the same demo
            organization from a different
            working perspective.
          </p>
        </header>

        <div
          className="demoRoleGrid"
        >
        {demoAccounts.map(
          (
            account,
            index
          ) => (
            <article
              key={
                account.key
              }
            >
              <span>
                {String(
                  index + 1
                ).padStart(
                  2,
                  "0"
                )}
              </span>

              <h2>
                {account.label}
              </h2>

              <p>
                {
                  account.description
                }
              </p>

              <Link
                className="portfolioPrimaryButton"
                href={`/demo/login/${account.key}`}
              >
                Enter demo as {
                  account.label
                }
              </Link>
            </article>
          )
        )}

        </div>
      </section>

      <section
        className="demoProjectPreview"
        aria-labelledby="demo-projects-heading"
      >

        <header>
          <span>
            DEMO PROJECTS
          </span>

          <h2
            id="demo-projects-heading"
          >
            Realistic construction scenarios
          </h2>
        </header>

        <div>
          <article>
            <strong>
              Rivoli Hotel Renovation
            </strong>
            <span>
              Paris · Renovation
            </span>
            <b>
              €1.85M
            </b>
          </article>

          <article>
            <strong>
              Belleville Office Fit-Out
            </strong>
            <span>
              Paris · Interior
            </span>
            <b>
              €640K
            </b>
          </article>

          <article>
            <strong>
              Neuilly Residence Extension
            </strong>
            <span>
              Neuilly-sur-Seine · Renovation
            </span>
            <b>
              €920K
            </b>
          </article>
        </div>
      </section>
    </main>
  );
}
