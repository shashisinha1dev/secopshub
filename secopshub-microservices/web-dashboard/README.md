# SecOpsHub Web Dashboard

React + TypeScript + Vite UI for the SecOpsHub Spring Boot microservices portfolio project.

## Prerequisites

- Node.js 20+ (Node 22 recommended)
- npm
- The backend modules running locally: API Gateway (`8080`), Incident Service (`8081`), Alert Ingestion (`8082`), Auth Service (`8083`), and RabbitMQ (`5672`)

## Run locally

Open a terminal in this `web-dashboard` directory:

```bash
npm install
npm run dev
```

Open http://localhost:5173. Vite proxies `/api/**` requests to the Spring Cloud Gateway at `http://localhost:8080`, avoiding local browser CORS configuration while developing.

## Demo users

These are demo-only in-memory accounts configured in `auth-service`:

| Role | Username | Password |
| --- | --- | --- |
| Incident Manager | `manager@secopshub.local` | `Manager@123` |
| SOC Analyst | `analyst@secopshub.local` | `Analyst@123` |
| Security Auditor | `auditor@secopshub.local` | `Auditor@123` |
| System Integration | `integration@secopshub.local` | `Integration@123` |

Change or remove all demo credentials before any public deployment. This application is for local portfolio demonstration, not production use as-is.

## Dashboard features

- JWT login, logout, session expiry check and role-aware actions
- Security overview cards, severity distribution and recent incidents
- Incident register search/filtering and create-incident modal
- Incident details, valid workflow status transitions, manager-only severity changes, audit timeline and related-incident view
- Alert inbox and simulated alert submission for the RabbitMQ workflow
- Quarterly reporting with fiscal-year and quarter selection

## Build

```bash
npm run build
```

## Backend/API assumptions

- API Gateway URL is `http://localhost:8080`.
- Auth endpoint: `POST /api/v1/auth/login`.
- Incident APIs use `/api/v1/incidents`, reports `/api/v1/reports/quarterly`, reference endpoints `/api/v1/reference/**`, and alerts `/api/v1/alerts`.
- Login response contains `accessToken`, `expiresIn`, `username`, and `roles`.
- JWT is held in `localStorage` for demo convenience. For a production architecture, evaluate a backend-for-frontend with secure HttpOnly cookies, TLS, stronger session controls, CSRF protections appropriate to the cookie strategy, and a persistent identity provider.

## Notes

Incident lists are currently loaded one page of up to 100 records at a time; pagination UI is illustrative and should be connected to server pagination before using large data volumes.
