package br.com.cmachado.parkingsystem.support;

import br.com.cmachado.parkingsystem.domain.shared.AggregateRootBase;
import br.com.cmachado.parkingsystem.domain.shared.DomainEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertions over the uncommitted domain events an aggregate has registered but not yet had
 * published. Events accumulate on {@link AggregateRootBase#domainEvents()} until a real save
 * clears them, so these checks are meaningful only before the aggregate is persisted.
 */
public final class DomainEventAssertions {

    private DomainEventAssertions() {
    }

    /** Returns the registered events of the given type, in registration order. */
    public static <E extends DomainEvent> List<E> eventsOf(AggregateRootBase<?> aggregate, Class<E> type) {
        return aggregate.domainEvents().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    /** Asserts the aggregate has registered at least one event of the given type. */
    public static void assertHasEvent(AggregateRootBase<?> aggregate, Class<? extends DomainEvent> type) {
        assertTrue(aggregate.domainEvents().stream().anyMatch(type::isInstance),
                () -> "expected a " + type.getSimpleName() + " on the uncommitted events list, but found "
                        + registeredTypes(aggregate));
    }

    /** Asserts the aggregate has registered no event of the given type. */
    public static void assertHasNoEvent(AggregateRootBase<?> aggregate, Class<? extends DomainEvent> type) {
        assertFalse(aggregate.domainEvents().stream().anyMatch(type::isInstance),
                () -> "expected no " + type.getSimpleName() + " on the uncommitted events list, but found "
                        + registeredTypes(aggregate));
    }

    private static List<String> registeredTypes(AggregateRootBase<?> aggregate) {
        return aggregate.domainEvents().stream()
                .map(event -> event.getClass().getSimpleName())
                .toList();
    }
}
