# AgentShield

**AI Agent Permission & Action Control Platform**

AgentShield sits between AI agents and the actions they want to take. Every agent gets its own API key, every action is checked against a per-agent permission policy, risky actions wait for a human to approve them, and everything is written to an audit log.

## How it works

1. **Register an agent.** You get back an API key once; only its SHA-256 hash is stored. Keys can be rotated.
2. **Set policies.** For each agent and action type, choose `ALLOW`, `DENY`, or `REQUIRE_APPROVAL`. With no policy set, the action is **denied by default**.
3. **Agent submits an action** with its key in the `X-Agent-API-Key` header.
   - `ALLOW` → executed right away (`ALLOWED` → `EXECUTED`)
   - `DENY` → rejected
   - `REQUIRE_APPROVAL` → parked as `PENDING_APPROVAL` until a human approves or rejects it (pending requests expire after 24 hours)
4. **Audit.** Every request, decision, and execution is recorded.

Supported action types: `READ_EMAIL`, `SEND_EMAIL`, `READ_FILE`, `DELETE_FILE`, `UPDATE_DATABASE`, `EXECUTE_REPORT`. Execution is simulated for now.

## Tech stack

- Java 21, Spring Boot 3.4
- Spring Web, Spring Data JPA, Bean Validation, Spring Security
- PostgreSQL 16 (Docker Compose), H2 for tests
- Lombok, springdoc OpenAPI (Swagger UI)
- JUnit 5, Spring Security Test, Testcontainers

## Getting started

Prerequisites: Java 21, Maven, Docker.

```bash
# 1. Set the required secrets (no defaults are shipped)
export AGENTSHIELD_DB_PASSWORD='choose-a-db-password'
export AGENTSHIELD_JWT_SECRET="$(openssl rand -base64 48)"

# 2. Start PostgreSQL
docker compose up -d

# 3. Run the app
mvn spring-boot:run
```

### Configuration

| Variable | Required | Default | Notes |
|---|---|---|---|
| `AGENTSHIELD_DB_PASSWORD` | yes | none | Also used by `docker compose` for the Postgres container |
| `AGENTSHIELD_JWT_SECRET` | yes | none | At least 32 bytes; startup fails with a clear message otherwise |
| `AGENTSHIELD_DB_URL` | no | `jdbc:postgresql://localhost:5432/agentshield` | |
| `AGENTSHIELD_DB_USERNAME` | no | `agentshield` | |
| `AGENTSHIELD_JWT_EXPIRATION_MS` | no | `86400000` (24 h) | |
| `PORT` | no | `8080` | |

The app runs on `http://localhost:8080`.

- Dashboard: `http://localhost:8080/`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `GET /health`

Demo users are seeded on startup (`demo@agentshield.com`, `admin@agentshield.com`). Disable this with `AGENTSHIELD_SEED_DEMO_USERS=false`, or override their passwords with `AGENTSHIELD_DEMO_USER_PASSWORD` / `AGENTSHIELD_DEMO_ADMIN_PASSWORD`.

## API overview

| Area | Method & path | Purpose |
|---|---|---|
| Agents | `POST /api/v1/agents` | Register an agent (returns its API key) |
| | `GET /api/v1/agents` · `GET /api/v1/agents/{agentId}` | List / view agents |
| | `PUT /api/v1/agents/{agentId}` · `DELETE /api/v1/agents/{agentId}` | Update / remove an agent |
| | `POST /api/v1/agents/{agentId}/rotate-api-key` | Issue a new API key |
| Policies | `POST /api/v1/agents/{agentId}/policies` | Add a permission policy |
| | `GET /api/v1/agents/{agentId}/policies` | List an agent's policies |
| | `DELETE /api/v1/agents/{agentId}/policies/{policyId}` | Remove a policy |
| Actions | `POST /api/v1/agent-actions` | Agent submits an action (`X-Agent-API-Key` header) |
| | `GET /api/v1/agent-actions` · `GET /api/v1/agent-actions/{requestId}` | List / view action requests |
| Approvals | `GET /api/v1/approvals/pending` | Actions waiting on a human |
| | `POST /api/v1/approvals/{requestId}/approve` · `.../reject` | Decide on a pending action |
| Audit | `GET /api/v1/audit-logs` | Full audit trail |

## Example

```bash
# Agent asks to send an email
curl -X POST http://localhost:8080/api/v1/agent-actions \
  -H "Content-Type: application/json" \
  -H "X-Agent-API-Key: <your-agent-key>" \
  -d '{
        "actionType": "SEND_EMAIL",
        "description": "Send weekly report to the team",
        "parameters": {"to": "team@example.com"}
      }'
```

## Running tests

```bash
mvn test
```

Tests run with the `test` profile (`src/test/resources/application-test.yml`): an in-memory H2 database and fixed test secrets, so no Docker or environment variables are needed.

## Roadmap

- JWT authentication for human users and role-based access (`ADMIN` / `USER`)
- Real action executors in place of the simulated one
- Database migrations (Flyway) instead of `ddl-auto: update`
