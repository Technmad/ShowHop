# ADR-0001: Integration tests run against the local docker-compose Postgres, not Testcontainers

## Status

Accepted.

## Context

The plan for this arc called for Testcontainers-managed PostgreSQL
containers for the `*IT` integration test suite, giving each test run a
fresh, isolated database.

In this environment (Docker Desktop on Windows, `desktop-linux` context),
Testcontainers' Docker Java client cannot open a working connection to the
daemon: both the `NpipeSocketClientProviderStrategy` and an explicit
`DOCKER_HOST=npipe:////./pipe/dockerDesktopLinuxEngine` override receive a
`400 Bad Request` with an empty/zero-valued `/info` response body, rather
than a real Docker Info payload. The plain `docker` CLI and `docker
compose` both work correctly against the same daemon over the same
context -- only Testcontainers' embedded `docker-java` HTTP client fails
to negotiate with it.

## Decision

Integration tests (`*IT.java`, run via `mvn verify` / the Failsafe plugin)
connect to the PostgreSQL instance started by `docker compose up -d db`
(see `docker-compose.yml`) instead of a Testcontainers-managed container.
Each `*IT` test:

- Points its datasource at `localhost:5432` (the compose service), not a
  dynamically-provisioned container.
- Skips itself (`Assumptions.assumeTrue`, reported as *skipped*, not
  *failed*) if that Postgres instance isn't reachable, rather than failing
  the build -- so `mvn verify` still succeeds in an environment where
  nobody has run `docker compose up -d db` yet.
- Is responsible for its own cleanup between runs (deleting the rows it
  created), since the database isn't torn down and recreated per test the
  way a fresh container would be.

The `org.testcontainers` dependencies have been removed rather than kept
declared-but-unused, since nothing in this codebase actually exercises
them.

## Consequences

- CI must bring up `docker compose up -d db` before `mvn verify` (see
  `.github/workflows/backend-ci.yml`).
- Test isolation is weaker than fresh-container-per-run: tests must clean
  up after themselves, and can't run with full parallelism against the
  same schema without risking cross-test interference.
- If a future environment has working Testcontainers support, reverting
  this decision is straightforward -- the tests' logic doesn't change,
  only how they obtain a Postgres connection.
