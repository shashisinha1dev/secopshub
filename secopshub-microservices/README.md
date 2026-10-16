# SecOpsHub — Security Incident Management Microservices

SecOpsHub is a Java 21 / Spring Boot portfolio project for managing security incidents and converting incoming security alerts into incidents asynchronously.

## Included applications

| Application | Port | Purpose |
|---|---:|---|
| API Gateway | 8080 | JWT validation, role-based authorization and routing |
| Incident Service | 8081 | Incident register, classification, severity, lifecycle, audit, relationships and quarterly reports |
| Alert Ingestion Service | 8082 | Alert intake API, deduplication/idempotency and event publishing |
| Auth Service | 8083 | Demo login, BCrypt password hashing and HS256 JWT issuance |
| RabbitMQ | 5672 | Asynchronous event transport; management UI on 15672 |

`event-contracts` is a shared Maven module containing the JSON event records used by both microservices.

## Requirements

- JDK 21
- IntelliJ IDEA with Maven support, or Maven 3.9+
- Docker Desktop for local RabbitMQ (or Docker Compose for the full containerized stack)
- Postman to exercise the APIs

## Option A — Run the services separately in IntelliJ IDEA

This is the recommended learning flow if you want each Java service in its own Run console.

1. Extract the ZIP and open the **root** `secopshub-microservices` folder in IntelliJ. Open the root `pom.xml` as a Maven project and wait for sync to finish. Keep `event-contracts`, `auth-service`, `incident-service`, `alert-ingestion-service`, and `api-gateway` in the same IntelliJ project.
2. Start RabbitMQ using a terminal in the project root (skip this if your existing RabbitMQ container is already healthy on port `5672`):

   ```powershell
   docker compose up -d rabbitmq
   ```

   Wait for RabbitMQ to become healthy. Management UI: `http://localhost:15672`. Local credentials: `secops` / `dev-password-change-me`.

3. Open and run each application class in its own IntelliJ Run configuration:
   - `com.secopshub.auth.AuthServiceApplication` — port `8083`
   - `com.secopshub.incident.IncidentServiceApplication` — port `8081`
   - `com.secopshub.alert.AlertIngestionApplication` — port `8082`
   - `com.secopshub.gateway.ApiGatewayApplication` — port `8080`
4. Leave all four applications running. Local mode uses separate in-memory H2 databases, so service data resets on restart. Incident and Alert services connect to RabbitMQ on `localhost:5672`.
5. If you prefer terminals instead of IntelliJ, run `mvn -DskipTests install` once from the project root, then start each service in its own terminal: `mvn -pl auth-service spring-boot:run`, `mvn -pl incident-service spring-boot:run`, `mvn -pl alert-ingestion-service spring-boot:run`, and `mvn -pl api-gateway spring-boot:run`.

If your services report connection errors for RabbitMQ, verify that Docker Desktop is running and `docker compose ps` shows the `rabbitmq` container as healthy. Do not use the default RabbitMQ `guest` account from another container or host; the project uses the local `secops` account.

## Option B — Run the complete stack in Docker Compose

Do **not** run the IntelliJ instances on the same ports at the same time as this option.

```powershell
docker compose up --build
```

This starts both PostgreSQL databases, RabbitMQ, the four Spring Boot applications and the Gateway. Data is persisted in Docker volumes. API ports are 8080, 8081, 8082 and 8083. Stop everything with `Ctrl+C`, then `docker compose down`; volumes are retained unless explicitly removed.

## Postman and demo logins

1. Import `postman/SecOpsHub.postman_collection.json` into Postman.
2. Keep the `baseUrl` collection variable set to `http://localhost:8080`.
3. Run **Login as Incident Manager (run first)**. The Tests script saves the returned `accessToken`; protected requests inherit `Authorization: Bearer {{accessToken}}` automatically. Optional analyst/auditor login requests at the bottom demonstrate role restrictions and overwrite the token; run the manager login again to restore all happy-path requests.
4. Then run the health checks and business requests. `GET /api/v1/health` and the service health endpoints are public for local diagnosis. Incident, report and alert APIs require a valid JWT.

Demo-only credentials (passwords are hashed with BCrypt in memory; don't use these accounts in production):

| Role | Username | Password | Typical permissions |
|---|---|---|---|
| SOC Analyst | `analyst@secopshub.local` | `Analyst@123` | Create/search incidents, update status, submit alerts |
| Incident Manager | `manager@secopshub.local` | `Manager@123` | Analyst actions plus change incident severity |
| Auditor | `auditor@secopshub.local` | `Auditor@123` | Read incidents, reports and audit history |
| System Integration | `integration@secopshub.local` | `Integration@123` | Submit and read alerts |

Login endpoint: `POST http://localhost:8080/api/v1/auth/login`. A successful login returns a signed HS256 JWT expiring after 30 minutes and a list of roles. The included default JWT secret is for local development only. Set `JWT_SECRET_BASE64` to a strong, randomly generated Base64-encoded secret of at least 32 bytes before any shared or cloud deployment.

After login, run `Submit simulated security alert (async)`. Expected: `202 Accepted`; then poll `Get alert processing status` until `INCIDENT_CREATED` and run `Get incident created from alert`.

## Key endpoints (through Gateway)

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Authenticate demo user and issue JWT |
| `GET` | `/api/v1/auth/health` | Auth Service health |
| `GET` | `/api/v1/health` | Incident Service health via Gateway |
| `GET` | `/api/v1/alerts/health` | Alert Ingestion Service health via Gateway |
| `POST` | `/api/v1/alerts` | Submit a normalized security alert |
| `GET` | `/api/v1/alerts` | List received alerts |
| `GET` | `/api/v1/alerts/{externalAlertId}` | Check alert processing status |
| `POST` | `/api/v1/incidents` | Manually create an incident |
| `GET` | `/api/v1/incidents/{incidentId}` | Get an incident |
| `GET` | `/api/v1/incidents` | Search incidents |
| `GET` | `/api/v1/reports/quarterly` | Quarterly report |

## Example alert request

```http
POST http://localhost:8080/api/v1/alerts
Content-Type: application/json
Authorization: Bearer {{accessToken}}
```

```json
{
  "externalAlertId": "DEMO-SIEM-10001",
  "source": "SIMULATED_SIEM",
  "title": "Repeated failed logins followed by unusual successful login",
  "description": "Simulated alert from a test connector.",
  "incidentType": "UNAUTHORIZED_ACCESS",
  "natureOfViolation": "ACCESS_CONTROL",
  "severity": "HIGH",
  "classificationStatus": "SUSPECTED",
  "reportedBy": "siem.connector@example.test",
  "assignedTo": "tier2.analyst@example.test",
  "businessUnit": "Information Technology",
  "policyReference": "IAM-STD-004",
  "occurredAt": "2026-10-16T09:05:00",
  "detectedAt": "2026-10-16T09:10:00",
  "reportedAt": "2026-10-16T09:20:00",
  "businessImpact": "Potential account compromise; impact not confirmed.",
  "rootCause": "",
  "correctiveAction": "Review sign-in events and verify MFA status."
}
```

Expected: `202 Accepted`. The alert will initially be `QUEUED`; after the Incident Service consumes the message and publishes the acknowledgement, it becomes `INCIDENT_CREATED` and includes the generated incident ID.

## Troubleshooting

- **Gateway gives 503 on alert routes:** confirm Alert Ingestion Service is running on 8082 and `secops.services.alert-uri` is correct.
- **Alert response is `PUBLISH_FAILED`:** confirm RabbitMQ is running, credentials are `secops` / `dev-password-change-me`, and both services can reach it. Submit again using the same `externalAlertId` to retry.
- **Alert stays `QUEUED`:** confirm Incident Service is running and RabbitMQ management UI shows messages being consumed from `secopshub.incident.alert.received`. Inspect `secopshub.incident.alert.received.dlq` if processing repeatedly fails.
- **401 Unauthorized:** run the Postman login request first; confirm the `accessToken` collection variable is non-empty and that all services use the same `JWT_SECRET_BASE64`.
- **403 Forbidden:** the logged-in role does not have permission for that operation. Use the Incident Manager demo account to change severity.
- **JWT decoder reports bad signature:** all four services must use exactly the same Base64 secret and issuer `secopshub-auth`.
- **Postman uses the wrong port:** always test through Gateway `8080`; direct service ports are mainly for local troubleshooting.

## Next steps for a production-grade portfolio

1. Add authentication persistence, account lifecycle and stronger key management; current demo users are in-memory.
2. Add unit/integration tests with Testcontainers, including JWT role rules, duplicate alerts and broker retries.
3. Add outbox/reliable publishing, schema versioning and structured observability.
4. Add evidence metadata/object storage, analyst tasks, SLA escalations and a React dashboard.
5. Deploy to Azure Container Apps with managed secrets, TLS, managed messaging and managed PostgreSQL.

This project is a development starter, not production-secured software. Do not send real employee, customer or security-event data into the demo.

## Web dashboard (React + TypeScript)

The `web-dashboard` folder contains the browser UI. Start the backend services and RabbitMQ first, then open a terminal in `web-dashboard` and run:

```powershell
npm install
npm run dev
```

Browse to `http://localhost:5173` and sign in using one of the demo accounts from the table above. The Vite development server proxies `/api/**` requests to the API Gateway at `http://localhost:8080`. See `WEB-DASHBOARD-QUICKSTART.md` and `web-dashboard/README.md` for UI features, setup, and security notes.
