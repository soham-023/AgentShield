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

### Changed
- Database credentials, JWT secret and server port are read from environment
  variables, with local development defaults.
- `SubmitActionRequest` validates description and idempotency key length to
  match the database column sizes.
