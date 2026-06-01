# ShowHop

ShowHop is an event ticketing platform: organizers create events and ticket
types, attendees discover and buy tickets, and staff validate tickets at the
door by QR code or manual entry.

## Status

Early development. See [`docs/PRD.md`](docs/PRD.md) for the full product
requirements, current-state notes, and roadmap.

## Repository layout

```
backend/    Spring Boot REST API (Java 21)
frontend/   React SPA (Vite + TypeScript)
docs/       Product and design documentation
```

## Local development

Prerequisites: JDK 21, Node 20+, Docker.

```bash
docker compose up -d      # PostgreSQL + Keycloak
cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev
```

## License

MIT — see [LICENSE](LICENSE).
