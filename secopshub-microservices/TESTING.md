# SecOpsHub automated testing guide

## What's covered

- `IncidentManagementServiceTest`: input normalization, audit event creation, duplicate-alert idempotency, invalid alert rejection, allowed/rejected status transitions, quarterly aggregation, malformed fiscal-year rejection, and not-found behaviour.
- `ReportingPeriodTest`: Indian April–March fiscal year quarter boundaries.
- `AlertIntakeServiceTest`: alert publish and queue status, no redundant publish for already-queued alerts, broker-publish failure behaviour, and incident-acknowledgement updates.
- `AuthControllerTest`: JWT response shape, deterministic sorted role names, failed authentication propagation, and the auth health response.
- Existing application context smoke tests remain for the Incident Service and API Gateway.

## Run all tests

Open CMD or PowerShell in the folder containing the root `pom.xml`, then run:

```powershell
mvn clean test
```

Individual modules:

```powershell
mvn -pl incident-service -am test
mvn -pl alert-ingestion-service -am test
mvn -pl auth-service test
mvn -pl api-gateway test
```

Use `-am` for incident and alert modules because they depend on the shared `event-contracts` Maven module.

## IntelliJ IDEA

1. Wait for Maven import/synchronization to finish.
2. Expand a module's `src/test/java` folder.
3. Right-click the test class or package and choose **Run Tests**.
4. Review the Run window; a green check means the selected tests passed.

These are unit tests and context smoke tests. The full test suite has not been executed in the artifact-generation environment because Maven is not installed there; run it in IntelliJ or a local Maven terminal to confirm against your resolved dependencies. Testcontainers integration coverage is the next recommended enhancement.
