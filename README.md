# KasiBridge AI Platform

An AI-powered platform that:
- Enables **informal township traders** to build verifiable digital business profiles
- Helps **municipal procurement officials** detect fraud and irregularities via AI analytics

## Architecture
Microservices built with **Spring Boot** + **Spring AI**, communicating via secure REST APIs.

## Services
| Service | Port | Status |
|---|---|---|
| trader-profile-service | 8081 | 🚧 In Progress |
| transaction-recording-service | 8082 | ⏳ Planned |
| ai-fraud-detection-service | 8083 | ⏳ Planned |
| whatsapp-adapter-service | 8084 | ⏳ Planned |
| payment-orchestrator-service | 8085 | ⏳ Planned |
| api-gateway | 8080 | ⏳ Planned |

## Branch Strategy
- `main` → stable releases only
- `dev` → integration of completed features
- `feature/*` → individual service development

## Tech Stack
- Java 21
- Spring Boot 3.2.x
- Spring AI
- PostgreSQL (H2 for dev)
- Twilio (WhatsApp)
- Docker (deployment)

## Getting Started
See each service's own `README.md` inside its folder.