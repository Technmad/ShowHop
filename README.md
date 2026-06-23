# ShowHop

ShowHop is an event ticketing platform: organizers create events and ticket
types, attendees discover and buy tickets, and staff validate tickets at the
door by QR code or manual entry.

## Status

`v0.1.0` — MVP complete: full event/ticket-type management, oversell-safe
purchasing, QR codes, and staff validation, across a Spring Boot backend
and a React frontend. See [`docs/PRD.md`](docs/PRD.md) for the full
product requirements, current-state notes, and roadmap, and
[`CHANGELOG.md`](CHANGELOG.md) for release notes.

## Repository layout

```
backend/    Spring Boot REST API (Java 21)
frontend/   React SPA (Vite + TypeScript)
docs/       Product and design documentation
```

## Local development

Prerequisites: JDK 21, Node 20+, Docker.

```bash
docker compose up -d      # PostgreSQL, Adminer, and Keycloak
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

Keycloak runs at `http://localhost:9090`. Create a realm named `showhop`,
a public client `showhop-app` with redirect URI
`http://localhost:5173/callback`, and realm roles `ORGANIZER`, `ATTENDEE`,
`STAFF` assigned to your test users.

## License

MIT — see [LICENSE](LICENSE).
