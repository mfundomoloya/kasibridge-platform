import { useMemo, useState } from "react";

import { Link } from "react-router-dom";

import { useAuth } from "../auth/useAuth";

import { PortalLayout } from "../components/layout/PortalLayout";

import type { NavigationItem } from "../components/layout/PortalLayout";

import type { UserRole } from "../types/auth";

import "./KasiBridgeShowcasePage.css";

type ScenarioKey = "LOW_RISK" | "HIGH_RISK";

interface IntegrityIndicator {
  label: string;
  normalValue: string;
  highRiskValue: string;
}

interface IntegrityScenario {
  title: string;
  score: string;
  classification: string;
  action: string;
  summary: string;
  reasons: string[];
}

interface WorkflowStage {
  number: string;
  title: string;
  description: string;
  control: string;
}

interface ModelSummary {
  name: string;
  role: string;
}

const PROCUREMENT_ROLES: UserRole[] = [
  "ROLE_ANALYST",
  "ROLE_SPECIFICATION_OFFICER",
  "ROLE_EVALUATOR",
  "ROLE_ADJUDICATOR",
  "ROLE_PLATFORM_ADMIN",
];

const SHOWCASE_NAVIGATION: NavigationItem[] = [
  {
    label: "Showcase",
    path: "/showcase",
  },
  {
    label: "Trader portal",
    path: "/dashboard",
    roles: ["ROLE_TRADER"],
  },
  {
    label: "Open tenders",
    path: "/tenders",
    roles: ["ROLE_TRADER"],
  },
  {
    label: "My bids",
    path: "/my-bids",
    roles: ["ROLE_TRADER"],
  },
  {
    label: "Procurement portal",
    path: "/procurement",
    roles: PROCUREMENT_ROLES,
  },
  {
    label: "AI assessments",
    path: "/procurement/ai-assessments",
    roles: ["ROLE_SPECIFICATION_OFFICER", "ROLE_PLATFORM_ADMIN"],
  },
  {
    label: "Fraud alerts",
    path: "/procurement/alerts",
    roles: ["ROLE_ANALYST"],
  },
  {
    label: "Administration",
    path: "/admin",
    roles: ["ROLE_PLATFORM_ADMIN"],
  },
];

const WORKFLOW_STAGES: WorkflowStage[] = [
  {
    number: "01",
    title: "Lock the specification",
    description:
      "A specification officer creates the tender. " +
      "KasiBridge hashes the approved specification " +
      "and detects later modifications.",
    control:
      "Standardised specifications and SHA-256 " + "integrity verification",
  },
  {
    number: "02",
    title: "Publish transparently",
    description:
      "The tender is published only after validation. " +
      "Eligible active traders are selected through " +
      "structured business-type and service-area matching.",
    control: "Deterministic matching and immutable publication audit",
  },
  {
    number: "03",
    title: "Notify matching traders",
    description:
      "A durable outbox record is created in the same " +
      "transaction. An approved Meta template delivers " +
      "the opportunity through WhatsApp.",
    control: "Transactional outbox, deduplication and delivery retry",
  },
  {
    number: "04",
    title: "Validate submitted bids",
    description:
      "Administrative compliance checks happen before " +
      "evaluation. A noncompliant bid cannot silently " +
      "progress to the award stage.",
    control: "Zero-discretion compliance gatekeeping",
  },
  {
    number: "05",
    title: "Evaluate anonymously",
    description:
      "Evaluators see controlled bidder aliases rather " +
      "than trader identities and record evidence-backed " +
      "technical scores.",
    control: "Double-blind evaluation and committee separation",
  },
  {
    number: "06",
    title: "Assess procurement integrity",
    description:
      "Rules and supervised machine learning review " +
      "qualification, conflicts, scoring, timing and " +
      "award-deviation indicators.",
    control: "Seven-classifier AI comparison and explainable findings",
  },
  {
    number: "07",
    title: "Control the award",
    description:
      "Critical rule violations block the award. Elevated " +
      "model risk requires independent review before " +
      "the tender can proceed.",
    control: "Human oversight with hard anti-corruption gates",
  },
  {
    number: "08",
    title: "Preserve the evidence",
    description:
      "Every action, rejection, override attempt and " +
      "notification is appended to the procurement " +
      "audit chain.",
    control: "Append-only, serialised and hash-linked audit records",
  },
];

const INTEGRITY_INDICATORS: IntegrityIndicator[] = [
  {
    label: "Winning bid compliance",
    normalValue: "Compliant",
    highRiskValue: "Failed compliance",
  },
  {
    label: "Qualification override",
    normalValue: "No override",
    highRiskValue: "Override attempted",
  },
  {
    label: "Committee conflict",
    normalValue: "No relationship found",
    highRiskValue: "Undeclared supplier link",
  },
  {
    label: "Highest score selected",
    normalValue: "Yes",
    highRiskValue: "Higher score bypassed",
  },
  {
    label: "Competition",
    normalValue: "Four valid bidders",
    highRiskValue: "Single effective bidder",
  },
  {
    label: "Award timing",
    normalValue: "Independent review completed",
    highRiskValue: "Awarded unusually quickly",
  },
  {
    label: "Award justification",
    normalValue: "Supported by evidence",
    highRiskValue: "Contradicts evaluation evidence",
  },
];

const MODEL_COMPARISON: ModelSummary[] = [
  {
    name: "Logistic Regression",
    role: "Explainable baseline",
  },
  {
    name: "Decision Tree",
    role: "Human-readable decisions",
  },
  {
    name: "Random Forest",
    role: "Nonlinear ensemble",
  },
  {
    name: "Support Vector Machine",
    role: "Margin classifier",
  },
  {
    name: "K-Nearest Neighbours",
    role: "Distance-based baseline",
  },
  {
    name: "Gaussian Naive Bayes",
    role: "Probabilistic baseline",
  },
  {
    name: "Gradient Boosting",
    role: "Sequential ensemble",
  },
];

const LOW_RISK_SCENARIO: IntegrityScenario = {
  title: "Normal competitive procurement",
  score: "18%",
  classification: "LOW RISK",
  action: "ALLOW WITH MONITORING",
  summary:
    "The proposed award follows compliance, " +
    "competition and evaluation controls.",
  reasons: [
    "The winning bid passed mandatory compliance.",
    "No committee-to-supplier conflict was detected.",
    "The highest validated score was selected.",
    "The award justification matches the evaluation evidence.",
  ],
};

const HIGH_RISK_SCENARIO: IntegrityScenario = {
  title: "Manipulation-risk scenario",
  score: "94%",
  classification: "CRITICAL RISK",
  action: "BLOCK AWARD AND REQUIRE REVIEW",
  summary:
    "The proposed award contains multiple " +
    "high-severity procurement-integrity " +
    "indicators.",
  reasons: [
    "A noncompliant bidder is being advanced.",
    "A qualification override was recorded.",
    "An undeclared committee-supplier link was detected.",
    "The highest-scoring compliant bid was bypassed.",
    "The award justification contradicts evaluation evidence.",
  ],
};

export function KasiBridgeShowcasePage() {
  const { user } = useAuth();

  const [selectedScenario, setSelectedScenario] =
    useState<ScenarioKey>("HIGH_RISK");

  const scenario = useMemo<IntegrityScenario>(() => {
    if (selectedScenario === "LOW_RISK") {
      return LOW_RISK_SCENARIO;
    }

    return HIGH_RISK_SCENARIO;
  }, [selectedScenario]);

  const isHighRisk = selectedScenario === "HIGH_RISK";

  const hasTraderRole = user?.roles.includes("ROLE_TRADER") ?? false;

  const hasProcurementRole =
    user?.roles.some((role) => PROCUREMENT_ROLES.includes(role)) ?? false;

  const hasAdministratorRole =
    user?.roles.includes("ROLE_PLATFORM_ADMIN") ?? false;

  function scrollToSection(sectionId: string): void {
    document.getElementById(sectionId)?.scrollIntoView({
      behavior: "smooth",
      block: "start",
    });
  }

  return (
    <PortalLayout
      portalName="KasiBridge Showcase"
      navigationItems={SHOWCASE_NAVIGATION}
    >
      <div className="showcase-page">
        <section className="showcase-hero">
          <div className="showcase-hero-copy">
            <span className="showcase-label">Presentation prototype</span>

            <h1>
              Transparent procurement.
              <span>Preventive integrity controls.</span>
            </h1>

            <p>
              KasiBridge protects the procurement lifecycle before money moves.
              The platform combines segregation of duties, locked
              specifications, anonymous evaluation, supervised AI, durable
              notifications and immutable audit evidence.
            </p>

              <div className="showcase-hero-actions">
                  <Link
                      className="showcase-primary-action"
                      to="/prototype"
                  >
                      Launch interactive prototype
                  </Link>

                  <button
                      className="showcase-secondary-action"
                      type="button"
                      onClick={() => {
                          scrollToSection(
                              'guided-workflow'
                          )
                      }}
                  >
                      Explore the workflow
                  </button>

                  <button
                      className="showcase-secondary-action"
                      type="button"
                      onClick={() => {
                          scrollToSection(
                              'integrity-demo'
                          )
                      }}
                  >
                      Run integrity scenario
                  </button>
              </div>

            <div className="showcase-session">
              <span>Showcase session</span>

              <strong>{user?.username ?? "Authenticated user"}</strong>

              <small>{user?.roles.join(", ") ?? "No roles available"}</small>
            </div>
          </div>

          <div className="showcase-hero-panel">
            <div className="showcase-live-row">
              <span className={"showcase-live-dot"} aria-hidden="true" />

              <span>Prototype capabilities</span>
            </div>

            <div className="showcase-hero-metric">
              <strong>8</strong>

              <span>Controlled procurement stages</span>
            </div>

            <div className="showcase-hero-metric">
              <strong>7</strong>

              <span>Supervised classifiers compared</span>
            </div>

            <div className="showcase-hero-metric">
              <strong>3</strong>

              <span>Independent committee functions</span>
            </div>

            <div className="showcase-assurance">
              <span>Verified live capability</span>

              <strong>
                Approved WhatsApp template delivered to a real device
              </strong>
            </div>
          </div>
        </section>

        <section className="showcase-statistics">
          <article>
            <span>Prevention point</span>

            <strong>Before award</strong>

            <p>
              Integrity assessment runs before the procurement decision becomes
              irreversible.
            </p>
          </article>

          <article>
            <span>Decision model</span>

            <strong>Hybrid</strong>

            <p>
              Deterministic controls remain authoritative while AI ranks complex
              risk combinations.
            </p>
          </article>

          <article>
            <span>Evidence design</span>

            <strong>Immutable</strong>

            <p>
              Audit events are append-only, serialised and cryptographically
              linked.
            </p>
          </article>

          <article>
            <span>Notification design</span>

            <strong>Durable</strong>

            <p>
              Business events and required communications commit atomically.
            </p>
          </article>
        </section>

        <section id="guided-workflow" className="showcase-section">
          <div className="showcase-section-heading">
            <div>
              <span className="showcase-label">Guided demonstration</span>

              <h2>One protected procurement journey</h2>
            </div>

            <p>
              Each stage creates evidence for the next stage. Users cannot skip
              a control simply by calling another endpoint.
            </p>
          </div>

          <div className="showcase-workflow">
            {WORKFLOW_STAGES.map((stage) => (
              <article className={"showcase-workflow-card"} key={stage.number}>
                <div className={"showcase-stage-number"}>{stage.number}</div>

                <div>
                  <h3>{stage.title}</h3>

                  <p>{stage.description}</p>

                  <span className={"showcase-control"}>{stage.control}</span>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section id="integrity-demo" className="showcase-section">
          <div className="showcase-section-heading">
            <div>
              <span className="showcase-label">AI procurement integrity</span>

              <h2>Compare two proposed awards</h2>
            </div>

            <p>
              The prototype predicts procurement risk. The prototype does not
              declare criminal guilt or replace independent investigation.
            </p>
          </div>

          <div className="scenario-selector">
            <button
              type="button"
              className={selectedScenario === "LOW_RISK" ? "active" : ""}
              onClick={() => {
                setSelectedScenario("LOW_RISK");
              }}
            >
              Normal procurement
            </button>

            <button
              type="button"
              className={
                selectedScenario === "HIGH_RISK" ? "active danger" : ""
              }
              onClick={() => {
                setSelectedScenario("HIGH_RISK");
              }}
            >
              Manipulation-risk scenario
            </button>
          </div>

          <div className="integrity-demo-grid">
            <article className={"integrity-evidence-card"}>
              <div className={"integrity-card-heading"}>
                <div>
                  <span className={"showcase-label"}>Evidence snapshot</span>

                  <h3>{scenario.title}</h3>
                </div>

                <span
                  className={
                    isHighRisk ? "risk-badge critical" : "risk-badge low"
                  }
                >
                  {scenario.classification}
                </span>
              </div>

              <div className="indicator-list">
                {INTEGRITY_INDICATORS.map((indicator) => (
                  <div className={"indicator-row"} key={indicator.label}>
                    <span>{indicator.label}</span>

                    <strong
                      className={
                        isHighRisk ? "indicator-risk" : "indicator-safe"
                      }
                    >
                      {isHighRisk
                        ? indicator.highRiskValue
                        : indicator.normalValue}
                    </strong>
                  </div>
                ))}
              </div>
            </article>

            <article
              className={
                isHighRisk
                  ? "integrity-result-card critical"
                  : "integrity-result-card safe"
              }
            >
              <span className="showcase-label">Model and rules result</span>

              <div className="risk-score">
                <strong>{scenario.score}</strong>

                <span>Estimated high-risk probability</span>
              </div>

              <h3>{scenario.action}</h3>

              <p>{scenario.summary}</p>

              <ul>
                {scenario.reasons.map((reason) => (
                  <li key={reason}>
                    <span aria-hidden="true">{isHighRisk ? "!" : "✓"}</span>

                    {reason}
                  </li>
                ))}
              </ul>

              <div className={"scenario-guardrail"}>
                <strong>Human-control boundary</strong>

                <p>
                  Hard compliance and conflict rules cannot be overridden by a
                  favourable model score.
                </p>
              </div>
            </article>
          </div>
        </section>

        <section className="showcase-section">
          <div className="showcase-section-heading">
            <div>
              <span className="showcase-label">Academic AI requirement</span>

              <h2>Seven supervised classifiers</h2>
            </div>

            <p>
              The final model will be selected from reproducible CSV training
              results using precision, recall, F1, ROC-AUC and PR-AUC.
            </p>
          </div>

          <div className="model-grid">
            {MODEL_COMPARISON.map((model, index) => (
              <article className="model-card" key={model.name}>
                <span>{String(index + 1).padStart(2, "0")}</span>

                <h3>{model.name}</h3>

                <p>{model.role}</p>
              </article>
            ))}
          </div>

          <div className="dataset-note">
            <div>
              <span className="showcase-label">Dataset checkpoint</span>

              <strong>300,000 procurement behavioural records loaded</strong>
            </div>

            <p>
              The current Kaggle source provides invoice-level behavioural
              features. Tender-process risk indicators will be added through a
              documented hybrid synthetic dataset for supervised classification.
            </p>
          </div>
        </section>

        <section className="showcase-section">
          <div className="showcase-section-heading">
            <div>
              <span className="showcase-label">
                Anti-corruption architecture
              </span>

              <h2>Controls that reinforce each other</h2>
            </div>
          </div>

          <div className="control-grid">
            <article>
              <span className="control-icon">01</span>

              <h3>Segregation of duties</h3>

              <p>
                Specification, evaluation and adjudication responsibilities are
                independently assigned.
              </p>
            </article>

            <article>
              <span className="control-icon">02</span>

              <h3>Qualification gate</h3>

              <p>
                Mandatory compliance evidence is evaluated before a bid
                proceeds.
              </p>
            </article>

            <article>
              <span className="control-icon">03</span>

              <h3>Double-blind scoring</h3>

              <p>
                Bidder identity is separated from the technical evaluation view.
              </p>
            </article>

            <article>
              <span className="control-icon">04</span>

              <h3>Specification integrity</h3>

              <p>
                Published tender content is protected by a stored SHA-256
                specification hash.
              </p>
            </article>

            <article>
              <span className="control-icon">05</span>

              <h3>Supervised AI</h3>

              <p>
                Multiple classifiers identify subtle combinations requiring
                independent review.
              </p>
            </article>

            <article>
              <span className="control-icon">06</span>

              <h3>Immutable audit</h3>

              <p>
                Attempts, failures and successful actions remain traceable
                through hash-linked audit evidence.
              </p>
            </article>
          </div>
        </section>

        <section className="showcase-launchpad">
          <div>
            <span className="showcase-label">
              Continue into the working platform
            </span>

            <h2>Open an operational KasiBridge portal</h2>

            <p>
              The showcase explains the system. These links open the existing
              role-protected workflows.
            </p>
          </div>

          <div className="launchpad-actions">
            {hasTraderRole && (
              <>
                <Link className={"showcase-primary-action"} to="/tenders">
                  Browse tenders
                </Link>

                <Link className={"showcase-secondary-action"} to="/my-bids">
                  View my bids
                </Link>
              </>
            )}

            {hasProcurementRole && (
              <Link className={"showcase-primary-action"} to="/procurement">
                Open procurement portal
              </Link>
            )}

            {hasAdministratorRole && (
              <Link className={"showcase-secondary-action"} to="/admin">
                Open administration
              </Link>
            )}
          </div>
        </section>
      </div>
    </PortalLayout>
  );
}
