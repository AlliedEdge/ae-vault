# Changelog - Infrastructure

All notable changes to the infrastructure configuration are documented here.

## [Unreleased]

### Changed
- No changes in this release

---

## [0.2.0] - 2026-08-05

### Added
- Docker infrastructure in infra/docker/:
  - docker-compose.yml with:
    - PostgreSQL database service
    - Redis cache service
    - Backend Spring Boot service
    - Frontend React service
    - Network configuration
    - Volume management
    - Environment variable configuration
    - Health checks
    - Restart policies
