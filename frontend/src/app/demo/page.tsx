import Link from "next/link";

import {
  demoAccounts,
  portfolioConfig,
} from "../../lib/portfolio-config";

export default function DemoPage() {
  return (
    <main className="demoExperience">
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
              Portfolio Demo
            </small>
          </div>
        </Link>

        <Link href="/">
          Back to product
        </Link>
      </nav>

      <section className="demoHero">
        <span className="portfolioEyebrow">
          INTERACTIVE ROLE-BASED DEMO
        </span>

        <h1>
          Explore BuildFlow without
          creating data manually.
        </h1>

        <p>
          NorthBuild Construction is
          pre-populated with three realistic
          construction projects. Choose the
          role you want to experience.
        </p>

        <p className="demoRoleSummary">
          Available demo roles:
          {" "}
          <strong>Owner</strong>,
          {" "}
          <strong>Project Manager</strong>,
          {" "}
          <strong>Site Manager</strong>,
          {" "}
          <strong>Accountant</strong>
          {" "}
          and
          {" "}
          <strong>Client</strong>.
        </p>

        <div className="demoOrganizationBadge">
          <strong>
            {
              portfolioConfig
                .demoOrganization
            }
          </strong>

          <span>
            3 portfolio projects
          </span>
        </div>
      </section>

      <section className="demoRoleGrid">
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

              <dl>
                <div>
                  <dt>
                    Email
                  </dt>
                  <dd>
                    {account.email}
                  </dd>
                </div>

                <div>
                  <dt>
                    Password
                  </dt>
                  <dd>
                    {
                      portfolioConfig
                        .demoPassword
                    }
                  </dd>
                </div>
              </dl>

              <Link
                className="portfolioPrimaryButton"
                href={`/demo/login/${account.key}`}
              >
                Login as {
                  account.label
                }
              </Link>
            </article>
          )
        )}
      </section>

      <section className="demoProjectPreview">
        <header>
          <span>
            DEMO PROJECTS
          </span>

          <h2>
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
