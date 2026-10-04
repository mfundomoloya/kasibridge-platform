# KasiBridge Platform

KasiBridge is an integrated procurement, trader support, and governance platform designed to help informal and township traders participate in formal procurement opportunities while giving buyers, evaluators, adjudicators, and platform administrators strong controls for fairness, auditability, and fraud prevention.

The platform combines trader profiles, transaction records, procurement workflows, compliance gatekeeping, double-blind evaluation, audit trails, anomaly detection, ticketing, notification workflows, and future WhatsApp/AI support into a modular microservices-based system.

---

## Project Vision

KasiBridge aims to bridge the gap between informal traders and formal procurement systems by making tender participation more accessible, transparent, and auditable.

The platform supports:

- Informal and township traders building verifiable digital business profiles
- Buyers publishing tenders and managing procurement lifecycles
- Traders submitting bids and tracking compliance outcomes
- Evaluators scoring bids without seeing bidder identities
- Adjudicators reviewing ranked outcomes and signing off awards
- Platform administrators monitoring audit trails, anomalies, and governance controls
- Future WhatsApp and AI-assisted support for low-friction trader access

---

## Core Features

### Trader Enablement

- Trader profile registration
- Business information management
- Transaction history recording
- Future document tracking and compliance expiry reminders
- Future WhatsApp-based tender alerts and lifecycle notifications

### Procurement Lifecycle

- Tender creation and publishing
- Specification hash generation
- Specification integrity verification
- Committee assignment
- Segregation of duties controls
- Bid submission
- Automated compliance gatekeeping
- Double-blind evaluation
- Formula-based price scoring
- Adjudication ranking
- Award sign-off
- Tender lifecycle stage controls

### Governance and Integrity Controls

- JWT-based identity and role enforcement
- Tender-scoped evaluator authorization
- Tender-scoped adjudicator authorization
- Zero-discretion compliance gatekeeping
- Audit event logging
- Audit hash-chain verification
- Procurement anomaly detection
- Persisted anomaly review and dismissal workflow
- Specification tamper detection

### Trader Support Roadmap

Planned support features include:

- Ticket logging for clarification requests
- Compliance appeal tickets
- Upload issue logging
- Official Q&A workflow
- Notification outbox
- WhatsApp-ready lifecycle messages
- AI-assisted ticket triage and draft responses

---

## Architecture

KasiBridge uses a modular microservices architecture built primarily with:

- Java 21
- Spring Boot
- Spring Security
- JWT authentication
- PostgreSQL
- Maven multi-module project structure
- REST APIs
- Future WhatsApp and AI integrations

Each service owns a clear domain responsibility and communicates through secure APIs.

---

## Services

| Service | Port | Status | Description |
|---|---:|---|---|
| `auth-service` | 8084 | ✅ Implemented | Handles users, roles, login, JWT issuing, and authentication. |
| `kasibridge-security-common` | N/A | ✅ Implemented | Shared JWT validation, public key loading, authentication filter, and security utilities. |
| `trader-profile-service` | 8081 | ✅ Implemented | Manages trader business profiles and trader onboarding data. |
| `transaction-recording-service` | 8082 | ✅ Implemented | Records trader business transactions and transaction summaries. |
| `ai-fraud-detection-service` | 8083 | ✅ Implemented | Supports fraud-risk analysis and transaction-related fraud indicators. |
| `procurement-service` | 8085 | ✅ In Progress | Handles tenders, bids, compliance gatekeeping, evaluation, adjudication, audit, anomalies, and procurement lifecycle controls. |
| `whatsapp-adapter-service` | TBD | ⏳ Planned | Future WhatsApp integration for trader notifications and conversational support. |
| `notification-service` / Notification Outbox | TBD | ⏳ Planned | Future notification queue for WhatsApp, email, SMS, and in-app messages. |
| `api-gateway` | 8080 | ⏳ Planned | Future unified gateway for routing external requests to services. |

---