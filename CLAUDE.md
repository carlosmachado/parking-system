# Parking System — project rules

Backend for managing a parking garage: spot availability, vehicle entry/exit, and revenue.
Events arrive from an external garage simulator via webhook.

## Stack

- Java 21, Spring Boot 3.4, Maven.
- MySQL (production), Flyway migrations; H2 for tests.
- Money via JSR-354 (Moneta), Lombok.

## Architecture

Domain-Driven Design with strict layering `presentation → application → domain`, plus
`infrastructure`. Always follow the [[ddd-patterns]] skill (`.claude/skills/ddd-patterns/`)
when touching domain types, events, services, repositories or controllers.

## API contract

- On startup, fetch the garage layout from the simulator `GET /garage` and persist sectors
  and spots. Retry while the simulator is not yet reachable.
- `POST /webhook` accepts `ENTRY`, `PARKED`, `EXIT` events and always replies `200`.
- `GET /revenue?date=<yyyy-MM-dd>&sector=<code>` returns
  `{ "amount": <BigDecimal>, "currency": "BRL", "timestamp": <ISO-8601> }`.
  `date` defaults to today; omitting `sector` aggregates all sectors.

## Business rules (authoritative)

- **ENTRY**: mark a spot occupied. If the garage is at 100% capacity, reject new entries
  until a spot is freed in any sector.
- **EXIT**: free the spot and charge the stay.
  - First 30 minutes are free.
  - Beyond 30 minutes, bill a flat rate **per started hour, first hour included**, using the
    sector `basePrice`, rounded up.
- **Dynamic pricing** by occupancy at exit time:
  - `< 25%` → 10% discount
  - `< 50%` → base price
  - `< 75%` → 10% surcharge
  - `≤ 100%` → 25% surcharge
- Sectors are logical divisions of one shared spot pool behind a single entrance gate group.

## Engineering rules

- Encode variant rules (e.g. pricing) as strategies + a factory, not inline conditionals.
- English Javadoc on public classes and non-trivial methods; no line-by-line comments.
- Add or update a test for any new business rule, transition, or endpoint behavior.
- Keep commits small and focused; Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`…).
- Run `./mvnw clean test` before committing.
