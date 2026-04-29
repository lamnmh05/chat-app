# Elastic Monitoring

This folder contains a local monitoring stack for the backend:

- Elasticsearch
- Kibana
- Elastic APM Server

## Start

```powershell
docker compose up -d
```

## Backend APM example

Run the backend with the Elastic APM Java agent:

```powershell
java `
  -javaagent:path\\to\\elastic-apm-agent.jar `
  -Delastic.apm.service_name=chat-app-backend `
  -Delastic.apm.server_url=http://localhost:8200 `
  -Delastic.apm.environment=local `
  -jar backend\\build\\libs\\backend-0.0.1-SNAPSHOT.jar
```

Kibana: `http://localhost:5601`
