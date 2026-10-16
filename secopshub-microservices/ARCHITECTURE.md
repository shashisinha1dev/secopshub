# SecOpsHub architecture

## Runtime services

- **API Gateway** (`8080`): single HTTP entry point; validates JWTs, enforces role-based endpoint access, and routes requests to business services.
- **Auth Service** (`8083`): authenticates local demo users, verifies BCrypt-hashed passwords, and issues signed 30-minute HS256 JWTs with role claims.
- **Incident Service** (`8081`): validates JWTs independently and owns the incident register, classification, severity, lifecycle transitions, audit events, incident relationships and quarterly reporting.
- **Alert Ingestion Service** (`8082`): validates JWTs independently, accepts normalized alerts, applies external-alert-ID idempotency, persists intake status, and publishes `AlertReceivedEvent` to RabbitMQ.
- **Event Contracts** (`event-contracts` Maven module): versionable payloads shared by event producers and consumers.
- **RabbitMQ** (`5672`; management UI `15672`): transports `AlertReceivedEvent` asynchronously and `IncidentCreatedForAlertEvent` back to the alert service.
- **PostgreSQL** in Docker Compose: a separate database for each data-owning service. Local IntelliJ mode defaults to separate in-memory H2 databases.

## Alert-to-incident workflow

1. Client calls `POST /api/v1/alerts` through Gateway.
2. Alert Ingestion Service saves the alert using the caller's `externalAlertId` as an idempotency key and publishes `AlertReceivedEvent`.
3. Incident Service consumes the message and inserts an incident with `sourceAlertId` set. A unique constraint prevents duplicate incidents from the same alert.
4. Incident Service publishes `IncidentCreatedForAlertEvent` after the incident service transaction commits.
5. Alert Ingestion Service consumes the acknowledgement and marks the alert `INCIDENT_CREATED`, including the incident ID.
6. Failed alert-consumer messages are retried and eventually sent to the `secopshub.incident.alert.received.dlq` dead-letter queue.

## Local ports

| Component | Address |
|---|---|
| Gateway | `http://localhost:8080` |
| Auth Service | `http://localhost:8083` |
| Incident Service | `http://localhost:8081` |
| Alert Ingestion Service | `http://localhost:8082` |
| RabbitMQ AMQP | `localhost:5672` |
| RabbitMQ management UI | `http://localhost:15672` |
| Incident PostgreSQL | `localhost:5432` |
| Alert PostgreSQL | `localhost:5433` |

## Important limitations

This is a development/portfolio starter, not a production SOC platform. The demo accounts are in memory, and HS256 verification uses a shared secret across services; before production use, adopt a managed identity provider, asymmetric signing/rotation, managed secrets, TLS, tenant separation, evidence-file storage, threat-intel integrations and operational alerting. The local H2 data is in-memory and resets when services restart. In the Docker Compose profile, each service uses its own PostgreSQL database.


## Authentication and roles

Local demo accounts authenticate at `POST /api/v1/auth/login`. The returned JWT includes a `roles` claim and expires after 30 minutes. The Gateway and both business services validate tokens independently. Demo users are stored in memory with BCrypt-hashed passwords; this is not production identity management.

- `SOC_ANALYST`: create/read incidents, transition status, submit/read alerts, read reports.
- `INCIDENT_MANAGER`: analyst permissions plus severity changes.
- `AUDITOR`: read incidents, reports and audit history.
- `SYSTEM_INTEGRATION`: submit and read alerts.

All services must use the same `JWT_SECRET_BASE64`; the checked-in default secret is for local development only.
