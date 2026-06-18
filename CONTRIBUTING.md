# Contributing to ShowHop

## Branching

- `main` is always green: builds and all tests pass.
- Work happens on short-lived branches: `feat/<slug>`, `fix/<slug>`,
  `chore/<slug>`, `test/<slug>`, `docs/<slug>`.
- Branches merge into `main` with a merge commit (`--no-ff`), not a
  fast-forward or squash, so the history keeps its topology.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <summary>

[optional body]
```

Types: `feat`, `fix`, `chore`, `test`, `docs`, `refactor`. Scope is usually
`backend` or `frontend` when a change is specific to one.

## Before opening a PR

- Backend unit/slice tests: `./mvnw test` from `backend/`.
- Backend integration tests too: `./mvnw verify`, which additionally runs
  `*IT` classes against a real PostgreSQL. Start it first with
  `docker compose up -d db` -- these tests skip themselves (not fail) if
  nothing is listening on `localhost:5432`. See docs/adr/0001 for why
  they target compose-managed Postgres rather than Testcontainers.
- Frontend: `npm run build` from `frontend/` (and `npm test` once a test
  runner is configured).

All must pass before merging.
