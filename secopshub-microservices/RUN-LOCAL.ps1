$ErrorActionPreference = "Stop"
$projectRoot = (Get-Location).Path

Write-Host "Starting RabbitMQ for local development..."
docker compose up -d rabbitmq
if ($LASTEXITCODE -ne 0) { throw "Could not start RabbitMQ. Start Docker Desktop and retry: docker compose up -d rabbitmq" }

Write-Host "Building all Maven modules..."
mvn -DskipTests install
if ($LASTEXITCODE -ne 0) { throw "Maven build failed. Fix the reported build errors before starting services." }

Write-Host "Starting Auth Service on http://localhost:8083"
Start-Process powershell -WorkingDirectory $projectRoot -ArgumentList '-NoExit','-Command','mvn -pl auth-service spring-boot:run'
Start-Sleep -Seconds 3
Write-Host "Starting Incident Service on http://localhost:8081"
Start-Process powershell -WorkingDirectory $projectRoot -ArgumentList '-NoExit','-Command','mvn -pl incident-service spring-boot:run'
Start-Sleep -Seconds 3
Write-Host "Starting Alert Ingestion Service on http://localhost:8082"
Start-Process powershell -WorkingDirectory $projectRoot -ArgumentList '-NoExit','-Command','mvn -pl alert-ingestion-service spring-boot:run'
Start-Sleep -Seconds 3
Write-Host "Starting API Gateway on http://localhost:8080"
mvn -pl api-gateway spring-boot:run
