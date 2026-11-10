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
- Database credentials, JWT secret and server port are read from environment
  variables, with local development defaults.
- `SubmitActionRequest` validates description and idempotency key length to
  match the database column sizes.
- Demo user seeding can be turned off with `AGENTSHIELD_SEED_DEMO_USERS=false`, and the
  demo passwords can be set with `AGENTSHIELD_DEMO_USER_PASSWORD` / `AGENTSHIELD_DEMO_ADMIN_PASSWORD`.
