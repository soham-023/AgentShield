# Changelog

All notable changes to AgentShield are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- README with overview, setup instructions and API documentation.
- GitHub Actions CI workflow running `mvn verify` on Java 21.
- Unit tests for `PolicyService` (create/update, delete, default-deny evaluation).
- Unit tests for `ApprovalService` (approve/reject, ownership, expiry, double-decision guards).
- `.editorconfig` for consistent formatting across editors.
- Multi-stage `Dockerfile` and `.dockerignore` for containerised deployment.
- Unit tests for `SimulatedActionExecutor` (status guards, duplicate-execution guard, every action type).
- Unit tests for `GlobalExceptionHandler` (404/400/403 mapping, 500 responses never leak exception messages).

### Changed
- Pending approvals now expire after `agentshield.approval.expiration-hours`
  (default 24), which was configured but ignored: the 24 hours was hard-coded.
- The `X-Agent-API-Key` header name lives in one constant, and stale "Phase 5"
  comments and Javadoc were replaced with what the code actually does.
- Services no longer fall back to the demo user when nobody is signed in (or the
  token's user was deleted); they return 401 and never auto-create a user.
- **Breaking:** the API is no longer open. Every `/api/v1` endpoint needs a user
  (JWT from the new `POST /api/v1/auth/login`, or HTTP Basic), except login, health,
  API docs and agent action submission (`X-Agent-API-Key`). Unauthenticated calls get a JSON 401.
- **Breaking:** `AGENTSHIELD_JWT_SECRET` and `AGENTSHIELD_DB_PASSWORD` are now required.
  The hard-coded defaults were removed; startup fails with a clear message if the JWT
  secret is missing or shorter than 32 bytes. Tests use a dedicated `test` profile.
- Database URL/username and server port are read from environment variables, with
  local development defaults.
- `SubmitActionRequest` validates description and idempotency key length to
  match the database column sizes.
- Demo user seeding can be turned off with `AGENTSHIELD_SEED_DEMO_USERS=false`, and the
  demo passwords can be set with `AGENTSHIELD_DEMO_USER_PASSWORD` / `AGENTSHIELD_DEMO_ADMIN_PASSWORD`.
