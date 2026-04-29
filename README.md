# Chat App

Mini Discord-style chat app for a course project.

## Modules

- `backend/`: Spring Boot + MongoDB + WebSocket/STOMP + JWT
- `frontend/`: JavaFX desktop client
- `load-test/`: k6 load test script

## Implemented scope

- Register, login, JWT auth, current user profile
- Guild create/list/join/leave/member list
- Channel create/list/update/delete
- Channel message history + realtime chat
- Presence updates + typing indicator
- File upload API + attachment-based messages
- Reaction, edit, delete message
- Direct conversation + direct message backend flow
- Backend monitoring config for Elastic APM stack

## Backend architecture

- `controller/`: REST + WebSocket entry points
- `service/`: business logic
- `repository/`: Spring Data MongoDB
- `domain/document/`: MongoDB collections
- `dto/`: request/response contracts
- `security/`: JWT filter, principal, security config
- `config/`: WebSocket, CORS, storage, properties

## Frontend architecture

- `client/`: HTTP client + token storage
- `service/`: REST services + STOMP over WebSocket client
- `store/`: app state stores
- `ui/`: login/register/main screens
- `model/`: DTOs mirrored from backend contracts

## Run backend

1. Start MongoDB on `mongodb://localhost:27017/chat_app`
2. Optional env:
   - `JWT_SECRET`
   - `FILE_UPLOAD_DIR`
   - `ELASTIC_APM_SERVER_URL`
3. Run:

```powershell
cd backend
.\gradlew.bat bootRun
```

## Run The Whole Project

From the repo root, one command is enough:

```powershell
.\run-project.cmd
```

What it does:

- starts Docker MongoDB container `chat-app-mongo` if needed
- starts backend in the background
- waits until backend health endpoint is up
- launches the JavaFX frontend

Useful variants:

```powershell
.\run-project.cmd -KeepBackendAlive
.\run-project.cmd -NoMongo
.\stop-project.cmd
.\stop-project.cmd -StopMongo
```

## Run frontend

```powershell
cd frontend
.\gradlew.bat run
```

Default API base URL is `http://localhost:8080`.

Override when needed:

```powershell
.\gradlew.bat run -Dchat.api.baseUrl=http://localhost:8080
```

## Monitoring

Monitoring assets are under `backend/monitoring/`.

Start the stack:

```powershell
cd backend\monitoring
docker compose up -d
```

Then run the backend with Elastic APM Java agent manually, or point your IDE run config to the APM server at `http://localhost:8200`.

## Load test

```powershell
k6 run load-test\k6-chat-test.js
```
