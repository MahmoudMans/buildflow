const features = [
  {
    title: "Project Operations",
    text: "Manage projects, teams, tasks and construction milestones."
  },
  {
    title: "Cost Control",
    text: "Track budgets, expenses, commitments and project performance."
  },
  {
    title: "Procurement",
    text: "Manage suppliers, purchase orders and material deliveries."
  },
  {
    title: "Client Experience",
    text: "Share progress, quotations, invoices and project decisions."
  }
];

export default function Home() {
  return (
    <main>
      <section className="hero">
        <nav className="navigation">
          <div className="brand">
            <span className="brandIcon">B</span>
            <span>BuildFlow</span>
          </div>

          <span className="phase">
            Foundation
          </span>
        </nav>

        <div className="heroContent">
          <div className="heroText">
            <p className="eyebrow">
              Construction operations platform
            </p>

            <h1>
              Build better projects.
              <span> Control every cost.</span>
            </h1>

            <p className="description">
              Manage construction projects,
              budgets, teams, suppliers and
              clients from one professional
              platform.
            </p>

            <div className="actions">
              <a
                className="primaryButton"
                href="#features"
              >
                Explore BuildFlow
              </a>

              <a
                className="secondaryButton"
                href="#technology"
              >
                Technology
              </a>
            </div>
          </div>

          <div className="projectCard">
            <div className="projectHeader">
              <div>
                <span className="label">
                  ACTIVE PROJECT
                </span>

                <h2>
                  Villa Renovation
                </h2>

                <p>Paris, France</p>
              </div>

              <span className="status">
                On Track
              </span>
            </div>

            <div className="progressHeader">
              <span>Project progress</span>
              <strong>72%</strong>
            </div>

            <div className="progressBar">
              <div className="progressValue" />
            </div>

            <div className="metrics">
              <article>
                <span>Contract</span>
                <strong>€188,000</strong>
              </article>

              <article>
                <span>Actual cost</span>
                <strong>€91,420</strong>
              </article>

              <article>
                <span>Open tasks</span>
                <strong>14</strong>
              </article>

              <article>
                <span>Milestone</span>
                <strong>Electrical</strong>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section
        className="features"
        id="features"
      >
        <div className="sectionTitle">
          <p className="eyebrow">
            Product foundation
          </p>

          <h2>
            One platform for construction operations.
          </h2>
        </div>

        <div className="featureGrid">
          {features.map((feature) => (
            <article
              className="featureCard"
              key={feature.title}
            >
              <h3>{feature.title}</h3>
              <p>{feature.text}</p>
            </article>
          ))}
        </div>
      </section>

      <section
        className="technology"
        id="technology"
      >
        <div>
          <p className="eyebrow">
            Engineering
          </p>

          <h2>
            Production-oriented foundation.
          </h2>
        </div>

        <div className="technologyList">
          <span>Next.js</span>
          <span>TypeScript</span>
          <span>Spring Boot</span>
          <span>Java 21</span>
          <span>PostgreSQL</span>
          <span>Docker</span>
        </div>
      </section>
    </main>
  );
}
