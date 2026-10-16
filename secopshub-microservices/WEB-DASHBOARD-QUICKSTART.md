# SecOpsHub Web Dashboard Quick Start

1. Keep the Spring Boot services and RabbitMQ running as described in the root README.
2. Open `web-dashboard` in a terminal (not in the Maven lifecycle).
3. Run `npm install` once.
4. Run `npm run dev`.
5. Open `http://localhost:5173`.
6. Sign in with `manager@secopshub.local` / `Manager@123` for the full demo workflow.

The UI calls `/api/v1/**` through Vite's development proxy to the API Gateway on port 8080. No Gateway CORS change should be necessary for local development.

See `web-dashboard/README.md` for features, API assumptions, and the demo user matrix.
