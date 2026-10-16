FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml pom.xml
COPY event-contracts/pom.xml event-contracts/pom.xml
COPY incident-service/pom.xml incident-service/pom.xml
COPY alert-ingestion-service/pom.xml alert-ingestion-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY event-contracts/src event-contracts/src
COPY incident-service/src incident-service/src
RUN mvn -B -pl incident-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/incident-service/target/incident-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
