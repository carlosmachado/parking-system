---
name: ddd-patterns
description: Enforce this project's Domain-Driven Design conventions — layered packages, aggregate roots with domain events, immutable value objects, application/domain services, and the strategy pattern for business rules. Use when adding or changing any domain type, event, service, repository, or controller.
---

# DDD Patterns

Conventions every change in this codebase must follow. When you add a domain type, event,
service, repository or controller, match the structure below exactly.

## Layering & dependencies

Dependencies point inward: `presentation → application → domain`, with `infrastructure`
implementing technical concerns. Never let `domain` import from `application`,
`presentation` or `infrastructure`.

```
domain/
  model/<aggregate>/        aggregates, entities, value objects, repositories, events
  service/<concern>/        domain services + strategies (pricing, occupancy)
  shared/                   base types: AggregateRootBase, DomainEvent, ValueObject, Entity, markers
application/<usecase>/      @ApplicationService interface + impl/ (orchestration, @Transactional)
infrastructure/            clients, http error handling, startup, config
presentation/controllers/rest/<resource>/   @RestController + request/response DTOs
```

## Aggregates

- Extend `AggregateRootBase<T>`; implement `sameIdentityAs`, `equals`, `hashCode` on identity.
- JPA needs a no-arg constructor: `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
- Real constructor is `private`/package; expose **static factory methods** as the public API
  (e.g. `VehicleEvent.enter(...)`). Validate invariants in the constructor/factory.
- State changes go through intention-revealing methods that guard transitions and
  **register domain events** — never public setters.

```java
@Entity
@Table(name = "vehicle_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VehicleEvent extends AggregateRootBase<VehicleEvent> {

    public static VehicleEvent enter(LicensePlate plate, LocalDateTime entryTime) {
        return new VehicleEvent(plate, entryTime);
    }

    public void exit(LocalDateTime exitTime, Money amount) {
        if (this.status == VehicleEventStatus.EXITED) {
            throw new IllegalStateException("Vehicle has already exited");
        }
        this.status = VehicleEventStatus.EXITED;
        registerEvent(new VehicleExitedDomainEvent(this, sectorCode, exitTime.toLocalDate(), amount));
    }
}
```

## Value objects

- Implement `ValueObject<T>`, annotate `@Embeddable`, keep immutable (no setters).
- Validate in the constructor; throw a domain exception on invalid input (see `Money` →
  `MoneyInvalidException`).
- Provide static factories where it reads better (`Money.of(...)`), normalize input so equal
  values compare equal (`LicensePlate`, `SectorCode` upper-case and strip noise).
- Implement `sameValueAs`, plus `equals`/`hashCode` over all attributes.

```java
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SectorCode implements ValueObject<SectorCode> {
    private String code;
    public SectorCode(String code) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("...");
        this.code = code.trim().toUpperCase();
    }
    @Override public boolean sameValueAs(SectorCode other) { return other != null && code.equals(other.code); }
}
```

## Domain events

- Extend `DomainEvent` (a Spring `ApplicationEvent`); carry only the data a handler needs
  (ids, value objects), not live aggregate references when avoidable.
- Aggregates `registerEvent(...)`; Spring Data publishes them on save via `@DomainEvents`.
- Handle side effects in a listener. Use `@TransactionalEventListener(AFTER_COMMIT)` +
  `@Async` + `@Transactional(REQUIRES_NEW)` so the side effect never blocks or rolls back
  the originating request (see `RevenueAsyncListener`).

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handleVehicleExited(VehicleExitedDomainEvent event) { ... }
```

## Services & repositories

- **Application service**: interface marked `@ApplicationService` with methods `@Transactional`,
  plus an `impl/` class. Orchestrates aggregates/domain services/repositories; holds no rules.
- **Domain service**: marked `@DomainService`, stateless, holds rules that span aggregates
  (e.g. `OccupancyDomainService`). Put logic that belongs to one aggregate on that aggregate.
- **Repository**: interface in the aggregate's package extending `JpaRepository<Agg, Id>`,
  annotated `@Repository`. Query methods use derived names; no business logic.

## Business rules → strategy pattern

Encode variant business rules as strategies behind an interface, selected by a factory, not
as `if/else` chains inside services. Pricing is the reference: `PricingStrategy` +
`BasePricingStrategy` + per-band subclasses + `PricingStrategyFactory`.

## Checklist before finishing

- [ ] New domain logic lives in `domain/`, free of Spring web/JPA-impl leakage.
- [ ] Aggregates mutate only through guarded methods; events registered on state change.
- [ ] Value objects immutable, validated, with `sameValueAs`/`equals`/`hashCode`.
- [ ] Use case wrapped in an `@ApplicationService` + `@Transactional`.
- [ ] English Javadoc on new public classes and non-trivial methods.
- [ ] A test covers the new rule or transition.
