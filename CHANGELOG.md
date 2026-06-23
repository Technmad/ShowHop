# Changelog

All notable changes to this project are documented in this file.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [0.1.0] — MVP

### Added

- **Domain**: User, Event, TicketType, Ticket, QrCode, TicketValidation
  entities with Flyway migrations.
- **Security**: Keycloak OIDC resource server, realm-role → Spring
  authority mapping, just-in-time user provisioning.
- **Organizer**: create/list/get/update/delete events; nested ticket-type
  CRUD, ownership-checked.
- **Attendee**: browse/search published events; oversell-safe ticket
  purchase (`PESSIMISTIC_WRITE` lock + status-aware sold count); list own
  tickets; QR code image per ticket.
- **Staff**: ticket validation by QR scan or manual entry, with duplicate-
  scan and cancelled-ticket rejection.
- **Frontend**: React SPA covering all three roles, Keycloak login via
  `react-oidc-context`.
- **Testing**: unit/slice test suite plus a real-PostgreSQL integration
  test (via `docker compose`, see `docs/adr/0001`) proving the purchase
  lock holds against the actual target database, not just an emulation.
- **CI**: GitHub Actions for backend (`mvn verify` against a Postgres
  service container) and frontend (`npm run build` + lint).

### Known limitations

See `docs/PRD.md` §10.
